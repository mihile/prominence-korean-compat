package local.prominence.memory;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.google.common.collect.ImmutableList;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import net.irisshaders.iris.helpers.StringPair;
import net.irisshaders.iris.shaderpack.include.AbsolutePackPath;
import net.irisshaders.iris.shaderpack.include.IncludeGraph;
import net.irisshaders.iris.shaderpack.include.IncludeProcessor;
import net.irisshaders.iris.shaderpack.preprocessor.JcppProcessor;

public final class ShaderSourceCacheTests {
    private static int checks;
    private static ShaderSourceDiskCache nativeCache;
    private static void check(boolean v,String m){checks++;if(!v)throw new AssertionError(m);}
    public static String bridgeNative(String source,Iterable<StringPair> defines,Operation<String> op){
        return ShaderPreprocessBridge.preprocessWithCache(nativeCache,source,defines,op);
    }
    private static String invoke(Method method,String source,List<StringPair> defines) throws Exception {
        try{return (String)method.invoke(null,source,defines);}catch(InvocationTargetException e){
            if(e.getCause() instanceof Exception x)throw x;throw e;
        }
    }
    public static void nativeTests(Class<?> transformed,Path work,Path shaderRoot) throws Exception {
        Path nativeDirectory=Files.createTempDirectory(work,"native-cache-");
        nativeCache=new ShaderSourceDiskCache(nativeDirectory,"native-fixture-v1");
        Method fixed=transformed.getMethod("glslPreprocessSource",String.class,Iterable.class);
        var defines=List.of(new StringPair("MC_VERSION","12001"),new StringPair("IS_IRIS","1"),new StringPair("IRIS_FEATURE_SEPARATE_HARDWARE_SAMPLERS","1"));
        var sources=new ArrayList<String>(List.of(
            "#version 330 core\n#define X 4\n#if X > 2\nfloat f=X;\n#endif\n",
            "#version 330 core\n#extension GL_ARB_shader_texture_lod : enable\n// test\nvoid main(){ gl_Position=vec4(1.0); }\n",
            "#version 330 core\n#if MC_VERSION == 12001\nconst int version=MC_VERSION;\n#endif\n"));
        // Resolve actual installed pack includes using the native Iris IncludeGraph/IncludeProcessor.
        var paths=ImmutableList.of(AbsolutePackPath.fromAbsolutePath("/world0/gbuffers_terrain.fsh"),
            AbsolutePackPath.fromAbsolutePath("/world0/composite.fsh"),AbsolutePackPath.fromAbsolutePath("/world-1/gbuffers_terrain.fsh"));
        IncludeGraph graph=new IncludeGraph(shaderRoot,paths);check(graph.getFailures().isEmpty(),"Native installed include graph failed");
        IncludeProcessor processor=new IncludeProcessor(graph);
        for(var path:paths){String source=String.join("\n",processor.getIncludedFile(path))+"\n";
            check(ShaderSourceDiskCache.safeInput(source,List.of()),"Actual installed GLSL was excluded: "+path);sources.add(source);}
        var outputs=new ArrayList<String>();long reference=System.nanoTime();for(String source:sources)outputs.add(JcppProcessor.glslPreprocessSource(source,defines));reference=System.nanoTime()-reference;
        long cold=System.nanoTime();
        for(int i=0;i<sources.size();i++)check(invoke(fixed,sources.get(i),defines).equals(outputs.get(i)),"Native preprocessor output changed");
        cold=System.nanoTime()-cold;nativeCache.awaitWrites();long hits=nativeCache.stats().hits();long warm=System.nanoTime();
        for(int i=0;i<sources.size();i++)check(invoke(fixed,sources.get(i),defines).equals(outputs.get(i)),"Persistent output mismatch");
        warm=System.nanoTime()-warm;check(nativeCache.stats().hits()==hits+sources.size(),"Actual transformed method failed to hit disk cache");
        nativeCache.close();nativeCache=new ShaderSourceDiskCache(nativeDirectory,"native-fixture-v1");
        for(int i=0;i<sources.size();i++)check(invoke(fixed,sources.get(i),defines).equals(outputs.get(i)),"New instance output differs");
        check(nativeCache.stats().hits()==sources.size(),"Cache does not survive a new instance");
        for(String directive:List.of("#warning visible diagnostic\n","#error visible diagnostic\n","#define X( 1\n")){
            long before=nativeCache.stats().writes();String source="#version 330 core\n"+directive;
            try{invoke(fixed,source,defines);}catch(RuntimeException expected){}
            nativeCache.awaitWrites();check(nativeCache.stats().writes()==before,"Diagnostic or failure cached");
            long prior=nativeCache.stats().hits();try{invoke(fixed,source,defines);}catch(RuntimeException expected){}
            check(nativeCache.stats().hits()==prior,"Diagnostic hidden by a cache hit");
        }
        // Native exception semantics, before native listener construction, must remain unchanged.
        try{invoke(fixed,"#warning IRIS_JCPP_GLSL_VERSION",defines);throw new AssertionError("Exploit check bypassed");}
        catch(RuntimeException expected){check(expected.getMessage().contains("exploit"),"Wrong native exception");}
        long repeatNative=System.nanoTime();for(int round=0;round<10;round++)for(int i=0;i<sources.size();i++)check(JcppProcessor.glslPreprocessSource(sources.get(i),defines).equals(outputs.get(i)),"Repeated native output mismatch");repeatNative=System.nanoTime()-repeatNative;
        long repeatCache=System.nanoTime();for(int round=0;round<10;round++)for(int i=0;i<sources.size();i++)check(invoke(fixed,sources.get(i),defines).equals(outputs.get(i)),"Repeated disk output mismatch");repeatCache=System.nanoTime()-repeatCache;
        System.out.println("CPU fixture sources="+sources.stream().map(String::length).toList()+", outputs="+outputs.stream().map(String::length).toList()+", stats="+nativeCache.stats());
        nativeCache.close();System.out.println("PASS: "+checks+" native transformed preprocessing/real pack includes/diagnostics/restart checks; nativeReferenceMs="+reference/1_000_000+", coldCacheMs="+cold/1_000_000+", warmCacheMs="+warm/1_000_000+", repeatedNativeMs="+repeatNative/1_000_000+", repeatedCacheMs="+repeatCache/1_000_000+" (CPU-only fixture, not game performance)");
    }
    public static void main(String[] args) throws Exception {
        Path dir=Files.createTempDirectory("shader-text-cache-test-");var defs=List.of(new ShaderSourceDiskCache.Define("A","1"));
        var count=new AtomicInteger();var compiler=(java.util.function.Supplier<ShaderSourceDiskCache.Compilation>)()->{count.incrementAndGet();return new ShaderSourceDiskCache.Compilation("result\n\u0000\ud800",true);};
        try(var cache=new ShaderSourceDiskCache(dir.resolve("cache"),"schema1")){
            String first=cache.preprocess("source",defs,compiler);cache.awaitWrites();
            check(cache.preprocess("source",defs,compiler).equals(first)&&count.get()==1,"Disk result/UTF16 changed or recompiled");
            String key=cache.key("source",defs);check(!key.equals(cache.key("source",List.of(new ShaderSourceDiskCache.Define("A","2")))),"Changed define key collision");
            check(!cache.key("ab",List.of(new ShaderSourceDiskCache.Define("c","d"))).equals(cache.key("a",List.of(new ShaderSourceDiskCache.Define("bc","d")))),"Length delimiter collision");
            check(!cache.key("s",List.of(new ShaderSourceDiskCache.Define("A","1"),new ShaderSourceDiskCache.Define("A","2"))).equals(cache.key("s",List.of(new ShaderSourceDiskCache.Define("A","2"),new ShaderSourceDiskCache.Define("A","1")))),"Macro order ignored");
            var file=dir.resolve("cache").resolve(key+".bin");byte[] good=Files.readAllBytes(file);
            // A fresh instance checks disk integrity independently of the warm memory cache.
            for(int at:new int[]{0,4,8,40,good.length-1}){byte[] broken=good.clone();broken[at]^=1;Files.write(file,broken);int before=count.get();try(var fresh=new ShaderSourceDiskCache(dir.resolve("cache"),"schema1")){fresh.preprocess("source",defs,compiler);fresh.awaitWrites();}check(count.get()==before+1,"Corrupt entry did not fall back at "+at);}
            Files.write(file,new byte[]{1,2});int before=count.get();try(var fresh=new ShaderSourceDiskCache(dir.resolve("cache"),"schema1")){fresh.preprocess("source",defs,compiler);fresh.awaitWrites();}check(count.get()==before+1,"Truncated entry accepted");
            for(String dynamic:List.of("__TIME__","__DATE__","__TIMESTAMP__","__COUNTER__","__FILE__","__BASE_FILE__","__INCLUDE_LEVEL__","A ## B","#include \"x\"","#/**/include \"x\"","#inc\\\nlude \"x\"","%:include \"x\"")){
                long b=cache.stats().bypasses();cache.preprocess(dynamic,defs,compiler);check(cache.stats().bypasses()==b+1,"Dynamic input cached: "+dynamic);}
            long b=cache.stats().bypasses();cache.preprocess("source",List.of(new ShaderSourceDiskCache.Define("A","__TIME__")),compiler);check(cache.stats().bypasses()==b+1,"Dynamic define cached");
            cache.preprocess("dirty",defs,()->new ShaderSourceDiskCache.Compilation("partial",false));cache.awaitWrites();check(!Files.exists(dir.resolve("cache").resolve(cache.key("dirty",defs)+".bin")),"Dirty output persisted");check(cache.stats().skippedDirty()>0,"Dirty counter missing");
            try{cache.preprocess("throw",defs,()->{throw new IllegalStateException("native failure");});throw new AssertionError("Exception swallowed");}catch(IllegalStateException expected){check(expected.getMessage().equals("native failure"),"Native exception replaced");}
            try(var changed=new ShaderSourceDiskCache(dir.resolve("cache"),"schema2")){check(!changed.key("source",defs).equals(key),"Native schema change ignored");}
            var pins=ShaderPreprocessGuard.pins();Map<String,String> versions=Map.of("minecraft","1.20.1","iris","1.7.6+mc1.20.1","org_anarres_jcpp","1.4.14");
            java.util.function.BiFunction<String,String,byte[]> classes=(mod,path)->{try(var in=ShaderSourceCacheTests.class.getClassLoader().getResourceAsStream(path)){return in==null?null:in.readAllBytes();}catch(Exception e){return null;}};
            check(ShaderPreprocessGuard.verifyPins(pins,versions::get,classes),"Installed native pins do not match");
            check(!ShaderPreprocessGuard.verifyPins(pins,id->id.equals("iris")?"changed":versions.get(id),classes),"Unknown version allowed");
            for(var mod:pins.entrySet())for(var pin:mod.getValue().getAsJsonObject().getAsJsonObject("classes").entrySet())
                check(!ShaderPreprocessGuard.verifyPins(pins,versions::get,(id,path)->path.equals(pin.getKey())?new byte[]{0}:classes.apply(id,path)),"Changed native class allowed "+pin.getKey());
        }
        try(var small=new ShaderSourceDiskCache(dir.resolve("bounded"),"bounded",1000,200,3)){
            Files.createDirectories(dir.resolve("bounded"));Path unrelated=dir.resolve("bounded/user.bin");Files.writeString(unrelated,"preserve");
            for(int i=0;i<10;i++){small.preprocess("s"+i,defs,()->new ShaderSourceDiskCache.Compilation("x".repeat(90),true));small.awaitWrites();}
            try(var files=Files.list(dir.resolve("bounded"))){var owned=files.filter(p->p.getFileName().toString().matches("[0-9a-f]{64}\\.bin")).toList();check(owned.size()<=3,"Entry limit exceeded");long total=0;for(Path p:owned)total+=Files.size(p);check(total<=1000,"Disk byte limit exceeded");}
            check(Files.readString(unrelated).equals("preserve"),"Unrelated file removed");check(small.stats().evictions()>0,"Eviction counter missing");
            long w=small.stats().writes();small.preprocess("oversize",defs,()->new ShaderSourceDiskCache.Compilation("x".repeat(101),true));small.awaitWrites();check(small.stats().writes()==w,"Oversized output queued");check(small.pendingBytes()==0,"Queued memory retained");check(small.stats().skippedOversize()>0,"Oversize counter missing");
        }
        try(var bounded=new ShaderSourceDiskCache(dir.resolve("memory-limit"),"memory-limit",8_000_000,4_000_000,8192)) {
            for(int i=0;i<20;i++)bounded.preprocess("bounded-source"+i,defs,()->new ShaderSourceDiskCache.Compilation("x".repeat(300_000),true));
            check(bounded.memoryBytes()<=8_000_000,"Memory cache exceeds byte bound");
            bounded.awaitWrites();
        }
        Path blocked=dir.resolve("blocked");Files.writeString(blocked,"not a directory");
        try(var cache=new ShaderSourceDiskCache(blocked.resolve("cache"),"blocked")){
            check(cache.preprocess("s",defs,compiler).startsWith("result"),"Unavailable disk stopped native compile");cache.awaitWrites();check(Files.readString(blocked).equals("not a directory"),"Blocking file changed");
        }
        try(var cache=new ShaderSourceDiskCache(dir.resolve("queue"),"queue")){
            var field=ShaderSourceDiskCache.class.getDeclaredField("writer");field.setAccessible(true);
            var executor=(java.util.concurrent.ThreadPoolExecutor)field.get(cache);var gate=new java.util.concurrent.CountDownLatch(1);
            executor.execute(()->{try{gate.await();}catch(InterruptedException e){Thread.currentThread().interrupt();}});
            try {for(int i=0;i<32;i++)cache.preprocess("queued"+i,defs,()->new ShaderSourceDiskCache.Compilation("q".repeat(200_000),true));
                check(cache.pendingBytes()>0&&cache.pendingBytes()<=16L*1024*1024,"Pending text byte budget exceeded");
                check(executor.getQueue().size()<=64,"Pending task count exceeded");
            }finally{gate.countDown();}
            cache.awaitWrites();check(cache.pendingBytes()==0,"Completed queue retains text budget");
            check(cache.indexScans()==1,"Burst rescanned disk directory per shader");
            check(cache.stats().writes()==32 && cache.stats().queued()==32 && cache.stats().droppedQueue()==0 && cache.stats().pendingBytes()==0,"Burst entries lost");
            long hitsBefore=cache.stats().hits();for(int i=0;i<32;i++){final int index=i;cache.preprocess("queued"+i,defs,()->{throw new AssertionError("Cache miss "+index);});}
            check(cache.stats().hits()-hitsBefore==32,"Burst not retained");
        }
        System.out.println("PASS: "+checks+" disk cache data/invalidation/bounds/native fingerprint checks; no game or OpenGL");
    }
}
