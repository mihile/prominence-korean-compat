package joinverification;

import batchverification.VerifyMixinTransforms;
import com.llamalad7.mixinextras.MixinExtrasBootstrap;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import local.prominence.join.MaterialMapCapacity;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.transformer.Config;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;

/** Uses installed classes but never initializes Minecraft, an audio device or an OpenGL context. */
public final class VerifyJoinTransforms {
    private static final class NativeLoader extends ClassLoader {
        final Map<String,byte[]> definitions;
        NativeLoader(Map<String,byte[]> d){super(VerifyJoinTransforms.class.getClassLoader());definitions=d;}
        protected synchronized Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException {
            Class<?> c=findLoadedClass(name);if(c==null&&definitions.containsKey(name)){byte[] b=definitions.get(name);c=defineClass(name,b,0,b.length);}
            if(c==null)c=super.loadClass(name,false);if(resolve)resolveClass(c);return c;
        }
    }
    public static int classLookups;
    public static Class<?> lookup(String n,boolean init,ClassLoader loader)throws ClassNotFoundException {classLookups++;return Class.forName(n,init,loader);}
    private static long idleCpu(byte[] bytes)throws Exception {
        var loader=new NativeLoader(Map.of("circuitlord.reactivemusic.PlayerThread",bytes));
        Class<?> cls=loader.loadClass("circuitlord.reactivemusic.PlayerThread");
        Thread t=(Thread)cls.getConstructor().newInstance();
        var bean=ManagementFactory.getThreadMXBean();boolean supported=bean.isThreadCpuTimeSupported();
        if(supported&&!bean.isThreadCpuTimeEnabled())bean.setThreadCpuTimeEnabled(true);
        long start=supported?bean.getThreadCpuTime(t.getId()):-1;
        try {Thread.sleep(250);return supported?bean.getThreadCpuTime(t.getId())-start:-1;}
        finally {var kill=cls.getDeclaredField("kill");kill.setAccessible(true);kill.setBoolean(t,true);t.interrupt();t.join(2000);if(t.isAlive())throw new AssertionError("Native idle music thread did not exit");}
    }
    public static void main(String[] args)throws Exception {
        Path out=Path.of(args[1]);Files.createDirectories(out);Path cfg=out.resolve("configs");Files.createDirectories(cfg);
        Files.writeString(cfg.resolve("offline-join.mixins.json"),Files.readString(Path.of(args[0])).replace("local.prominence.join.JoinGuard",VerifyMixinTransforms.OfflinePlugin.class.getName()));
        var urls=new ArrayList<URL>();urls.add(cfg.toUri().toURL());for(String p:System.getProperty("java.class.path").split(File.pathSeparator))urls.add(Path.of(p).toUri().toURL());
        var resources=new URLClassLoader(urls.toArray(URL[]::new),VerifyJoinTransforms.class.getClassLoader());
        var f=VerifyMixinTransforms.class.getDeclaredField("LOADER");f.setAccessible(true);f.set(null,resources);
        MixinBootstrap.init();Method phase=MixinEnvironment.class.getDeclaredMethod("gotoPhase",MixinEnvironment.Phase.class);phase.setAccessible(true);phase.invoke(null,MixinEnvironment.Phase.INIT);
        MixinExtrasBootstrap.init();MixinEnvironment.getCurrentEnvironment().setObfuscationContext("named:intermediary");Mixins.addConfiguration("offline-join.mixins.json");
        for(Config c:Mixins.getConfigs()){c.getConfig().decorate(FabricUtil.KEY_MOD_ID,"prominence_korean_compat");c.getConfig().decorate(FabricUtil.KEY_COMPATIBILITY,FabricUtil.COMPATIBILITY_LATEST);}
        phase.invoke(null,MixinEnvironment.Phase.DEFAULT);MixinEnvironment.getCurrentEnvironment().setObfuscationContext("named:intermediary");
        f=VerifyMixinTransforms.class.getDeclaredField("TRANSFORMER");f.setAccessible(true);var transform=(IMixinTransformer)f.get(null);
        var defs=new HashMap<String,byte[]>();byte[] musicOriginal=null,musicFixed=null;int count=0;boolean gate=false,wake=false,capacity=false,predicates=false;int filenameHooks=0;
        for(String name:List.of("circuitlord.reactivemusic.PlayerThread","de.keksuccino.fancymenu.util.mcef.MCEFUtil","net.irisshaders.iris.shaderpack.materialmap.BlockMaterialMapping","net.fabricmc.fabric.impl.resource.loader.ModNioResourcePack","net.minecraft.class_3258")) {
            byte[] raw;try(var in=resources.getResourceAsStream(name.replace('.','/')+".class")){raw=in.readAllBytes();}
            byte[] fixed=transform.transformClassBytes(name,name,raw);if(Arrays.equals(raw,fixed))throw new AssertionError("No transform: "+name);
            var node=new ClassNode();new ClassReader(fixed).accept(node,0);
            if(name.endsWith("BlockMaterialMapping")){
                var before=new ClassNode();new ClassReader(raw).accept(before,0);
                if(!Arrays.equals(methodBytes(before,"checkState"),methodBytes(node,"checkState")))throw new AssertionError("Native simple predicate comparison was changed");
            }
            for(var m:node.methods){new Analyzer<BasicValue>(new BasicVerifier()).analyze(node.name,m);count++;
                for(var i:m.instructions)if(i instanceof MethodInsnNode call){
                    if(call.owner.equals("local/prominence/join/IdleSongWait")&&call.name.equals("await"))gate=true;
                    if(call.owner.equals("local/prominence/join/IdleSongWait")&&call.name.equals("signal"))wake=true;
                    if(call.owner.equals("local/prominence/join/MaterialMapCapacity"))capacity=true;
                    if(call.owner.equals("local/prominence/join/MaterialPredicateScope"))predicates=true;
                    if(call.owner.equals("local/prominence/join/ResourceFilename")&&call.name.equals("tryFormat"))filenameHooks++;
                    if(name.contains("MCEFUtil")&&call.owner.equals("java/lang/Class")&&call.name.equals("forName")){call.owner="joinverification/VerifyJoinTransforms";call.name="lookup";}
                }
            }
            if(name.contains("PlayerThread")){musicOriginal=raw;musicFixed=fixed;}
            var writer=new ClassWriter(0);node.accept(writer);defs.put(name,writer.toByteArray());Files.write(out.resolve(name+".class"),fixed);
        }
        if(!gate||!wake||!capacity||!predicates||filenameHooks!=2)throw new AssertionError("Required native hooks missing");
        // Real original and transformed run loops, with no song queued: neither opens audio/game.
        long originalCpu=idleCpu(musicOriginal),patchedCpu=idleCpu(musicFixed);
        var loader=new NativeLoader(defs);Class<?> menu=loader.loadClass("de.keksuccino.fancymenu.util.mcef.MCEFUtil");var query=menu.getMethod("isMCEFLoaded");
        if((Boolean)query.invoke(null))throw new AssertionError("Fixture requires MCEF absent");int first=classLookups;
        for(int i=0;i<200;i++)if((Boolean)query.invoke(null))throw new AssertionError("Presence changed");
        if(first!=1||classLookups!=1)throw new AssertionError("Repeated missing class lookup");
        menu.getField("MCEF_critical_failure").setBoolean(null,true);if((Boolean)query.invoke(null))throw new AssertionError("Critical flag ignored");
        menu.getField("MCEF_critical_failure").setBoolean(null,false);if((Boolean)query.invoke(null))throw new AssertionError("Absent state lost");
        Class<?> material=loader.loadClass("net.irisshaders.iris.shaderpack.materialmap.BlockMaterialMapping");var mapClass=it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap.class;
        MaterialMapCapacity.completed(50_000);Object result=material.getMethod("createBlockStateIdMap",mapClass,mapClass).invoke(null,new it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap<>(),new it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap<>());
        var n=result.getClass().getDeclaredField("n");n.setAccessible(true);if(n.getInt(result)!=it.unimi.dsi.fastutil.HashCommon.arraySize(50_000,.75f))throw new AssertionError("Native constructor capacity not changed");
        if(MaterialMapCapacity.expected()!=16)throw new AssertionError("Native completed size not observed");
        String report="{\"status\":\"passed\",\"targets\":5,\"methods\":"+count+",\"musicOriginalIdleCpuNs\":"+originalCpu+",\"musicPatchedIdleCpuNs\":"+patchedCpu+",\"idleWindowMs\":250,\"nativeMenuCalls\":203,\"classLookups\":"+classLookups+",\"gameStarted\":false,\"audioOpened\":false}";
        Files.writeString(out.resolve("native-report.json"),report);System.out.println(report);
    }
    private static byte[] methodBytes(ClassNode owner,String name){var one=new ClassNode();one.version=Opcodes.V17;one.access=Opcodes.ACC_PUBLIC;one.name="NativeComparison";one.superName="java/lang/Object";one.methods.add(owner.methods.stream().filter(m->m.name.equals(name)).findFirst().orElseThrow());var raw=new ClassWriter(0);one.accept(raw);var normalized=new ClassNode();new ClassReader(raw.toByteArray()).accept(normalized,ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);var w=new ClassWriter(ClassWriter.COMPUTE_MAXS);normalized.accept(w);return w.toByteArray();}

}
