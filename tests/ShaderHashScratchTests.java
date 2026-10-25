package local.prominence.memory;

import java.lang.management.ManagementFactory;
import java.net.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;

public final class ShaderHashScratchTests {
    static int checks;
    static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
    static String reference(String namespace,String source,List<ShaderSourceDiskCache.Define> defines)throws Exception {
        var md=MessageDigest.getInstance("SHA-256");update(md,namespace);update(md,source);md.update(java.nio.ByteBuffer.allocate(4).putInt(defines.size()).array());
        for(var d:defines){update(md,d.key());update(md,d.value());}return HexFormat.of().formatHex(md.digest());
    }
    static void update(MessageDigest md,String s){md.update(java.nio.ByteBuffer.allocate(4).putInt(s.length()).array());for(int i=0;i<s.length();i++){char c=s.charAt(i);md.update((byte)(c>>>8));md.update((byte)c);}}
    public static void main(String[]args)throws Exception {
        Path dir=Files.createTempDirectory("shader-hash-fixture-");String ns="scratch-fixture-v1";
        try(var cache=new ShaderSourceDiskCache(dir,ns)) {
            for(String source:List.of("","abc","a\u0000b","\ud800x\udfff","한글😀","x".repeat(8193)))for(var defs:List.of(List.<ShaderSourceDiskCache.Define>of(),List.of(new ShaderSourceDiskCache.Define("A",""),new ShaderSourceDiskCache.Define("A","\ud800"))))check(cache.key(source,defs).equals(reference(ns,source,defs)),"hash protocol changed");
            var defs=new ArrayList<ShaderSourceDiskCache.Define>();for(int i=0;i<500;i++)defs.add(new ShaderSourceDiskCache.Define("D_"+i,"1"));
            String expected=reference(ns,"void main(){}",defs);check(cache.key("void main(){}",defs).equals(expected),"many-defines changed");
            var pool=Executors.newFixedThreadPool(3);
            try{var futures=new ArrayList<Future<String>>();for(int i=0;i<30;i++)futures.add(pool.submit(()->cache.key("void main(){}",defs)));for(var f:futures)check(f.get().equals(expected),"shared mutable scratch race");}finally{pool.shutdownNow();}
            var bean=(com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();bean.setThreadAllocatedMemoryEnabled(true);
            // Compare production before/after classes in isolation, not a hand-written allocation estimate.
            if(args.length>0)try(var loader=new URLClassLoader(new URL[]{Path.of(args[0]).toUri().toURL()},ClassLoader.getPlatformClassLoader())) {
                Class<?> old=loader.loadClass("local.prominence.memory.ShaderSourceDiskCache"),define=loader.loadClass("local.prominence.memory.ShaderSourceDiskCache$Define");
                Object baseline=old.getConstructor(Path.class,String.class).newInstance(dir,ns);var ctor=define.getConstructor(String.class,String.class);var oldDefs=new ArrayList<Object>();for(int i=0;i<500;i++)oldDefs.add(ctor.newInstance("D_"+i,"1"));
                var key=old.getDeclaredMethod("key",String.class,List.class);key.setAccessible(true);
                try {
                    for(int i=0;i<3;i++){key.invoke(baseline,"void main(){}",oldDefs);cache.key("void main(){}",defs);}
                    long tid=Thread.currentThread().getId(),before=bean.getThreadAllocatedBytes(tid);for(int i=0;i<5;i++)check(key.invoke(baseline,"void main(){}",oldDefs).equals(expected),"baseline mismatch");long oldBytes=bean.getThreadAllocatedBytes(tid)-before;
                    before=bean.getThreadAllocatedBytes(tid);for(int i=0;i<5;i++)check(cache.key("void main(){}",defs).equals(expected),"current mismatch");long newBytes=bean.getThreadAllocatedBytes(tid)-before;
                    check(newBytes*10<oldBytes,"hash scratch reduction not demonstrated");System.out.println("Hash allocation fixture (5 calls / 500 defines): beforeBytes="+oldBytes+", afterBytes="+newBytes+"; not a game RAM measurement.");
                }finally{old.getMethod("close").invoke(baseline);}
            }
        }
        System.out.println("PASS: "+checks+" hash compatibility, malformed UTF-16, buffer boundary and concurrency checks"+(args.length>0?", including production allocation comparison":"")+"; no game.");
    }
}
