package local.prominence.memory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/** Immutable preprocessed text only. Never owns a world, pack, parser or GPU object. */
public final class ShaderSourceDiskCache implements AutoCloseable {
    public record Define(String key, String value) {}
    public record Compilation(String text, boolean clean) {}
    public record Stats(long hits, long misses, long bypasses, long writes, long readNanos, long nativeNanos, long skippedDirty, long skippedOversize, long droppedQueue, long writeFailures, long evictions, long queued, long pendingBytes) {}
    private static final int MAGIC=0x50475332, HEADER=72;
    private static final long PENDING_LIMIT=16L*1024*1024;
    private final Path directory;
    private final String namespace;
    private final long maxBytes;
    private final int maxEntry, maxEntries;
    private final ThreadPoolExecutor writer;
    private final java.util.LinkedHashMap<String,String> memoryCache = new java.util.LinkedHashMap<>(64,0.75f,true);
    private long memoryBytes;
    private final long memoryLimit;
    private final AtomicLong pending=new AtomicLong(), hits=new AtomicLong(), misses=new AtomicLong(), bypasses=new AtomicLong();
    private final AtomicLong writes=new AtomicLong(), readNanos=new AtomicLong(), nativeNanos=new AtomicLong();
    private final AtomicLong skippedDirty=new AtomicLong(), skippedOversize=new AtomicLong(), droppedQueue=new AtomicLong(), writeFailures=new AtomicLong(), evictions=new AtomicLong(), queued=new AtomicLong();
    private volatile boolean closed;
    private volatile boolean readDirectoryReady;

    public ShaderSourceDiskCache(Path directory,String namespace) {
        this(directory,namespace,1536L*1024*1024,4*1024*1024,8192);
    }
    ShaderSourceDiskCache(Path directory,String namespace,long maxBytes,int maxEntry,int maxEntries) {
        this.directory=directory.toAbsolutePath().normalize();this.namespace=namespace;
        this.maxBytes=maxBytes;this.maxEntry=maxEntry;this.maxEntries=maxEntries;
        this.memoryLimit=Math.min(maxBytes,32L*1024*1024);
        writer=new ThreadPoolExecutor(1,2,5,TimeUnit.SECONDS,new LinkedBlockingQueue<>(64),r->{
            Thread t=new Thread(r,"Prominence shader text cache");t.setDaemon(true);t.setPriority(Thread.NORM_PRIORITY-1);return t;
        },new ThreadPoolExecutor.AbortPolicy());
    }
    public String preprocess(String source,List<Define> defines,Supplier<Compilation> nativeCompiler) {
        if(closed || !safeInput(source,defines)) {bypasses.incrementAndGet();return nativeCompiler.get().text();}
        String key=key(source,defines);
        String mem=memoryGet(key);
        if(mem!=null){hits.incrementAndGet();return mem;}
        long start=System.nanoTime();String cached=read(key);readNanos.addAndGet(System.nanoTime()-start);
        if(cached!=null){
            hits.incrementAndGet();
            remember(key,cached);
            return cached;
        }
        misses.incrementAndGet();start=System.nanoTime();Compilation result;
        try {result=nativeCompiler.get();} finally {nativeNanos.addAndGet(System.nanoTime()-start);}
        if(result.clean() && result.text()!=null) {
            remember(key,result.text());
            enqueue(key,result.text());
        } else skippedDirty.incrementAndGet();
        return result.text();
    }
    // These builtins can depend on time/filesystem; token pasting can manufacture their names.
    static boolean safeInput(String source,List<Define> defines) {
        if(source==null || source.length()>4*1024*1024 || defines==null || defines.size()>4096 || dynamic(source))return false;
        for(Define d:defines)if(d==null || d.key()==null || d.value()==null || dynamic(d.key()) || dynamic(d.value()))return false;
        return true;
    }
    private static boolean dynamic(String s) {
        // Do not retain complete shader source strings in a process-global memo table.
        return s.contains("__DATE__")||s.contains("__TIME__")||s.contains("__TIMESTAMP__")||s.contains("__COUNTER__")
            ||s.contains("__FILE__")||s.contains("__BASE_FILE__")||s.contains("__INCLUDE_LEVEL__")||s.contains("##")
            ||s.contains("%:")||s.contains("__has_include")||externalInclude(s)
            ||s.contains("IRIS_JCPP_GLSL_VERSION")||s.contains("IRIS_JCPP_GLSL_EXTENSION");
    }
    private synchronized String memoryGet(String key){return memoryCache.get(key);}
    private synchronized void remember(String key,String text){
        long bytes=2L*text.length();if(bytes>memoryLimit)return;
        String previous=memoryCache.put(key,text);if(previous!=null)memoryBytes-=2L*previous.length();memoryBytes+=bytes;
        var entries=memoryCache.entrySet().iterator();
        while((memoryBytes>memoryLimit||memoryCache.size()>maxEntries)&&entries.hasNext()){
            var entry=entries.next();memoryBytes-=2L*entry.getValue().length();entries.remove();
        }
    }
    synchronized long memoryBytes(){return memoryBytes;}
    private static boolean externalInclude(String s) {
        if(s.indexOf('#')<0)return false;
        if(!s.contains("include")&&!s.contains("\\\n")&&!s.contains("\\\r"))return false;
        boolean block=false,line=false;char quote=0;
        for(int i=0;i<s.length();i++){
            char c=s.charAt(i),next=i+1<s.length()?s.charAt(i+1):0;
            if(block){if(c=='*'&&next=='/'){block=false;i++;}continue;}
            if(line){if(c=='\n'||c=='\r')line=false;else continue;}
            if(quote!=0){if(c=='\\')i++;else if(c==quote)quote=0;continue;}
            if(c=='/'&&next=='*'){block=true;i++;continue;}
            if(c=='/'&&next=='/'){line=true;i++;continue;}
            if(c=='\"'||c=='\''){quote=c;continue;}
            if(c!='#')continue;
            StringBuilder directive=new StringBuilder();int j=i+1;
            while(j<s.length()){
                char d=s.charAt(j),n=j+1<s.length()?s.charAt(j+1):0;
                if(d=='\\'&&(n=='\n'||n=='\r')){j+=2;if(n=='\r'&&j<s.length()&&s.charAt(j)=='\n')j++;continue;}
                if(d=='/'&&n=='*'){int end=s.indexOf("*/",j+2);if(end<0)break;j=end+2;continue;}
                if(d==' '||d=='\t'){if(directive.length()>0)break;j++;continue;}
                if(!Character.isJavaIdentifierPart(d))break;directive.append(d);j++;
            }
            if(directive.toString().startsWith("include"))return true;
        }
        return false;
    }
    static byte[] hash(byte[] data) {
        try{return MessageDigest.getInstance("SHA-256").digest(data);}catch(Exception e){throw new IllegalStateException(e);}
    }
    private static void length(MessageDigest md,int length,byte[] scratch) {
        scratch[0]=(byte)(length>>>24);scratch[1]=(byte)(length>>>16);scratch[2]=(byte)(length>>>8);scratch[3]=(byte)length;
        md.update(scratch,0,4);
    }
    private static void update(MessageDigest md,String s,byte[] chunk) {
        // Encode UTF-16 code units with lengths: unambiguous even for malformed surrogates.
        length(md,s.length(),chunk);
        int offset=0;
        for(int i=0;i<s.length();i++){char c=s.charAt(i);chunk[offset++]=(byte)(c>>>8);chunk[offset++]=(byte)c;
            if(offset==chunk.length){md.update(chunk);offset=0;}}
        if(offset!=0)md.update(chunk,0,offset);
    }
    String key(String source,List<Define> defines) {
        try {MessageDigest md=MessageDigest.getInstance("SHA-256");byte[] scratch=new byte[8192];update(md,namespace,scratch);update(md,source,scratch);
            length(md,defines.size(),scratch);
            for(Define d:defines){update(md,d.key(),scratch);update(md,d.value(),scratch);}
            return HexFormat.of().formatHex(md.digest());
        }catch(Exception e){throw new IllegalStateException(e);}
    }
    private boolean safeDirectory(boolean create) throws IOException {
        // Reject junctions/symlinks in the cache path before reads, writes or pruning.
        Path cursor=directory.getRoot();
        for(Path part:directory){cursor=cursor.resolve(part);
            if(Files.exists(cursor,LinkOption.NOFOLLOW_LINKS)) {
                if(!Files.isDirectory(cursor,LinkOption.NOFOLLOW_LINKS)||Files.isSymbolicLink(cursor))return false;
                if(!cursor.toRealPath().equals(cursor.toAbsolutePath().normalize()))return false;
            }else if(create)Files.createDirectory(cursor);else return false;
        }
        return true;
    }
    private String read(String key) {
        try {
            if(!readDirectoryReady){
                if(!Files.isDirectory(directory,LinkOption.NOFOLLOW_LINKS)||!safeDirectory(false))return null;
                readDirectoryReady=true;
            }
            Path file=directory.resolve(key+".bin");
            if(!Files.isRegularFile(file,LinkOption.NOFOLLOW_LINKS))return null;
            long size=Files.size(file);if(size<HEADER||size>maxEntry+HEADER)return null;
            byte[] all;
            // A bounded read prevents another process growing a file into an unbounded allocation.
            try(var in=Files.newInputStream(file,LinkOption.NOFOLLOW_LINKS)){all=in.readNBytes(maxEntry+HEADER+1);}
            if(all.length!=size)return null;ByteBuffer b=ByteBuffer.wrap(all);
            if(b.getInt()!=MAGIC)return null;int chars=b.getInt();byte[] storedKey=new byte[32],digest=new byte[32];b.get(storedKey);b.get(digest);
            if(chars<0||chars>maxEntry/2||b.remaining()!=chars*2||!MessageDigest.isEqual(storedKey,HexFormat.of().parseHex(key)))return null;
            byte[] payload=new byte[b.remaining()];b.get(payload);if(!MessageDigest.isEqual(digest,hash(payload)))return null;
            return ByteBuffer.wrap(payload).asCharBuffer().toString();
        }catch(IOException|SecurityException|IllegalArgumentException e){return null;}
    }
    private void enqueue(String key,String text) {
        long bytes=2L*text.length();if(bytes>maxEntry||bytes+HEADER>maxBytes||closed){skippedOversize.incrementAndGet();return;}
        long total=pending.addAndGet(bytes);if(total>PENDING_LIMIT){pending.addAndGet(-bytes);droppedQueue.incrementAndGet();return;}
        try{writer.execute(()->{try{write(key,text);}finally{pending.addAndGet(-bytes);}});queued.incrementAndGet();}
        catch(RuntimeException e){pending.addAndGet(-bytes);droppedQueue.incrementAndGet();}
    }
    private void write(String key,String text) {
        Path temporary=null;
        try {
            if(!safeDirectory(true)){writeFailures.incrementAndGet();return;}readDirectoryReady=true;int bytes=text.length()*2;
            prune(bytes+HEADER);
            byte[] payload=new byte[bytes];ByteBuffer.wrap(payload).asCharBuffer().put(text);
            ByteBuffer b=ByteBuffer.allocate(HEADER+bytes);b.putInt(MAGIC).putInt(text.length());b.put(HexFormat.of().parseHex(key));b.put(hash(payload));b.put(payload);
            temporary=Files.createTempFile(directory,"writing-",".tmp");Files.write(temporary,b.array());
            Files.move(temporary,directory.resolve(key+".bin"),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);temporary=null;writes.incrementAndGet();
        }catch(IOException|SecurityException e){writeFailures.incrementAndGet(); /* Optional cache failure keeps native output. */}
        finally {if(temporary!=null)try{Files.deleteIfExists(temporary);}catch(IOException|SecurityException ignored){}}
    }
    private record Entry(Path path,long size,long time) {}
    private void prune(long incoming) throws IOException {
        var entries=new ArrayList<Entry>();long size=0;
        try(var temporary=Files.newDirectoryStream(directory,"writing-*.tmp")){
            long cutoff=System.currentTimeMillis()-TimeUnit.DAYS.toMillis(1);
            for(Path p:temporary)if(p.getFileName().toString().matches("writing-[0-9]+\\.tmp")&&Files.isRegularFile(p,LinkOption.NOFOLLOW_LINKS)
                &&Files.getLastModifiedTime(p,LinkOption.NOFOLLOW_LINKS).toMillis()<cutoff)Files.deleteIfExists(p);
        }
        try(var stream=Files.newDirectoryStream(directory,"*.bin")){
            for(Path p:stream)if(p.getFileName().toString().matches("[0-9a-f]{64}\\.bin")&&Files.isRegularFile(p,LinkOption.NOFOLLOW_LINKS)){
                var a=Files.readAttributes(p,java.nio.file.attribute.BasicFileAttributes.class,LinkOption.NOFOLLOW_LINKS);
                entries.add(new Entry(p,a.size(),a.lastModifiedTime().toMillis()));size+=a.size();
            }
        }
        entries.sort(Comparator.comparingLong(Entry::time));int count=entries.size();
        for(Entry entry:entries){if(size+incoming<=maxBytes&&count<maxEntries)break;
            if(Files.deleteIfExists(entry.path()))evictions.incrementAndGet();size-=entry.size();count--;}
    }
    public Stats stats(){return new Stats(hits.get(),misses.get(),bypasses.get(),writes.get(),readNanos.get(),nativeNanos.get(),skippedDirty.get(),skippedOversize.get(),droppedQueue.get(),writeFailures.get(),evictions.get(),queued.get(),pending.get());}
    long pendingBytes(){return pending.get();}
    void awaitWrites() throws InterruptedException {
        long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(30);
        while(pending.get()!=0&&System.nanoTime()<end)Thread.sleep(5);
        if(pending.get()!=0)throw new IllegalStateException("Cache writer did not drain");
    }
    public void close(){closed=true;writer.shutdown();}
}
