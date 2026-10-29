package workverification;

import batchverification.VerifyMixinTransforms;
import com.llamalad7.mixinextras.MixinExtrasBootstrap;
import java.io.File;
import java.lang.reflect.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.transformer.*;

/** Executes native AE2 iteration and native Minecraft item-stack set with small platform/factory probes. */
public final class VerifyCreativeTransforms {
    private static final class NativeLoader extends ClassLoader {
        final Map<String,byte[]> defs;
        NativeLoader(Map<String,byte[]> d){super(VerifyCreativeTransforms.class.getClassLoader());defs=d;}
        protected synchronized Class<?> loadClass(String n,boolean resolve)throws ClassNotFoundException{
            Class<?> c=findLoadedClass(n);if(c==null&&defs.containsKey(n)){byte[] b=defs.get(n);c=defineClass(n,b,0,b.length);}if(c==null)c=super.loadClass(n,false);if(resolve)resolveClass(c);return c;
        }
    }
    private static byte[] headless(byte[] raw) {
        var node=new ClassNode();new ClassReader(raw).accept(node,0);
        // Unused native bootstrap methods require the full client registry. Iteration and wrappers stay native.
        node.methods.removeIf(m->m.name.equals("init")||m.name.equals("getDisplayItems")||m.name.equals("lambda$init$0"));
        var w=new ClassWriter(0);node.accept(w);return w.toByteArray();
    }
    public static void main(String[]args)throws Exception{
        Path out=Path.of(args[1]);Files.createDirectories(out);Path cfg=out.resolve("configs");Files.createDirectories(cfg);
        Files.writeString(cfg.resolve("creative.mixins.json"),Files.readString(Path.of(args[0])).replace("local.prominence.work.WorkGuard",VerifyMixinTransforms.OfflinePlugin.class.getName()));
        Files.writeString(cfg.resolve("old-index.mixins.json"),"{\"required\":true,\"package\":\"local.prominence.emiindex.mixin\",\"compatibilityLevel\":\"JAVA_17\",\"client\":[\"EmiStackIndexScopeMixin\"],\"injectors\":{\"defaultRequire\":1}}");
        var urls=new ArrayList<URL>();urls.add(cfg.toUri().toURL());for(String p:System.getProperty("java.class.path").split(File.pathSeparator))urls.add(Path.of(p).toUri().toURL());var resources=new URLClassLoader(urls.toArray(URL[]::new),VerifyCreativeTransforms.class.getClassLoader());
        Field f=VerifyMixinTransforms.class.getDeclaredField("LOADER");f.setAccessible(true);f.set(null,resources);MixinBootstrap.init();Method phase=MixinEnvironment.class.getDeclaredMethod("gotoPhase",MixinEnvironment.Phase.class);phase.setAccessible(true);phase.invoke(null,MixinEnvironment.Phase.INIT);MixinExtrasBootstrap.init();MixinEnvironment.getCurrentEnvironment().setObfuscationContext("named:intermediary");
        Mixins.addConfiguration("old-index.mixins.json");Mixins.addConfiguration("creative.mixins.json");
        for(Config config:Mixins.getConfigs()){config.getConfig().decorate(FabricUtil.KEY_MOD_ID,"prominence_korean_compat");config.getConfig().decorate(FabricUtil.KEY_COMPATIBILITY,FabricUtil.COMPATIBILITY_LATEST);}
        phase.invoke(null,MixinEnvironment.Phase.DEFAULT);MixinEnvironment.getCurrentEnvironment().setObfuscationContext("named:intermediary");f=VerifyMixinTransforms.class.getDeclaredField("TRANSFORMER");f.setAccessible(true);var transformer=(IMixinTransformer)f.get(null);
        byte[] facadeOriginal=null,facadeFixed=null;int methods=0;boolean scope=false,dedup=false,timing=false,oldIndex=false;
        for(String target:List.of("appeng.core.FacadeCreativeTab","dev.emi.emi.registry.EmiStackList")){
            byte[] raw;try(var in=resources.getResourceAsStream(target.replace('.','/')+".class")){raw=in.readAllBytes();}byte[] fixed=transformer.transformClassBytes(target,target,raw);if(Arrays.equals(raw,fixed))throw new AssertionError("Missing transform "+target);
            var node=new ClassNode();new ClassReader(fixed).accept(node,0);
            if(target.endsWith("EmiStackList")){
                var before=new ClassNode();new ClassReader(raw).accept(before,0);int oldSubmit=0,newSubmit=0;
                for(var m:before.methods)for(var i:m.instructions)if(i instanceof MethodInsnNode call&&call.owner.equals("net/minecraft/class_310")&&call.name.equals("method_5385"))oldSubmit++;
                for(var m:node.methods)for(var i:m.instructions)if(i instanceof MethodInsnNode call){
                    if(call.owner.equals("net/minecraft/class_310")&&call.name.equals("method_5385"))newSubmit++;
                    if(call.owner.equals("java/util/concurrent/ForkJoinPool")||(call.owner.equals("java/lang/Thread")&&call.name.equals("sleep")))throw new AssertionError("Creative callbacks moved/delayed");
                }
                if(oldSubmit==0||oldSubmit!=newSubmit)throw new AssertionError("Native creative client executor call lost");
            }
            for(var m:node.methods){new Analyzer<BasicValue>(new BasicVerifier()).analyze(node.name,m);methods++;
                for(var i:m.instructions.toArray())if(i instanceof MethodInsnNode call){scope|=call.owner.equals("local/prominence/work/FacadeBuildScope")&&call.name.equals("begin");dedup|=call.owner.equals("local/prominence/work/FacadeBuildScope")&&call.name.equals("first");timing|=call.owner.equals("org/slf4j/Logger")&&call.name.equals("info");oldIndex|=call.owner.equals("local/prominence/emiindex/EmiIndexBuildScope");}
            }
            Files.write(out.resolve(target+".class"),fixed);
            if(target.equals("appeng.core.FacadeCreativeTab")){facadeOriginal=headless(raw);facadeFixed=headless(fixed);}
        }
        if(!scope||!dedup||!timing||!oldIndex)throw new AssertionError("Native cumulative hooks missing");
        var fixture=new HashMap<String,byte[]>();Path fixtureDir=Path.of(args[2]);try(var paths=Files.walk(fixtureDir)){for(var p:paths.filter(p->p.toString().endsWith(".class")).toList())fixture.put(fixtureDir.relativize(p).toString().replace(File.separatorChar,'.').replaceAll("\\.class$",""),Files.readAllBytes(p));}
        for(String n:List.of("net.minecraft.class_7708","net.minecraft.class_7708$1","local.prominence.work.FacadeBuildScope"))try(var in=resources.getResourceAsStream(n.replace('.','/')+".class")){fixture.put(n,in.readAllBytes());}
        List<String> original=null,optimized=null;int originalCalls=0,optimizedCalls=0;
        for(boolean fixed:List.of(false,true)){
            var defs=new HashMap<>(fixture);defs.put("appeng.core.FacadeCreativeTab",fixed?facadeFixed:facadeOriginal);var loader=new NativeLoader(defs);var runner=loader.loadClass("fixture.CreativeFixture");
            @SuppressWarnings("unchecked") var result=(List<String>)runner.getMethod("run",boolean.class).invoke(null,fixed);
            int calls=runner.getField("firstCalls").getInt(null);
            if(fixed){optimized=result;optimizedCalls=calls;}else{original=result;originalCalls=calls;}
        }
        if(!Objects.equals(original,optimized)||optimizedCalls>=originalCalls)throw new AssertionError("Native output/order or optimization failed");
        String report="{\"status\":\"passed\",\"targets\":2,\"asmMethods\":"+methods+",\"originalCalls\":"+originalCalls+",\"optimizedCalls\":"+optimizedCalls+",\"nativeOutputAndOrderEqual\":true,\"nativeMinecraftItemStackSet\":true,\"nativeIterationAndExceptionHandling\":true,\"itemAndBlockFactoryProbes\":true,\"unusedBootstrapOmittedForExecution\":true,\"oldEmiIndexWrapperPreserved\":true,\"gameStarted\":false}";Files.writeString(out.resolve("native-report.json"),report);System.out.println(report);
    }
}
