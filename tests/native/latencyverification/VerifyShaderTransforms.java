package latencyverification;

import batchverification.VerifyMixinTransforms;
import com.llamalad7.mixinextras.MixinExtrasBootstrap;
import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import local.prominence.memory.ShaderSourceCacheTests;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.BasicVerifier;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.FabricUtil;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.transformer.Config;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;

/** Native CPU-only transformation/execution fixture. Does not construct a game, world or GL context. */
public final class VerifyShaderTransforms {
    private static final class NativeLoader extends ClassLoader {
        final Map<String,byte[]> definitions;
        NativeLoader(Map<String,byte[]> definitions){super(VerifyShaderTransforms.class.getClassLoader());this.definitions=definitions;}
        protected synchronized Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException {
            Class<?> c=findLoadedClass(name);if(c==null&&definitions.containsKey(name)){byte[] b=definitions.get(name);c=defineClass(name,b,0,b.length);}
            if(c==null)c=super.loadClass(name,false);if(resolve)resolveClass(c);return c;
        }
    }
    public static void main(String[] args)throws Exception {
        Path work=Path.of(args[1]);Files.createDirectories(work);
        Path configs=work.resolve("configs");Files.createDirectories(configs);
        Files.writeString(configs.resolve("offline-shader-cache.mixins.json"),Files.readString(Path.of(args[0]))
            .replace("local.prominence.memory.ShaderPreprocessGuard",VerifyMixinTransforms.OfflinePlugin.class.getName()));
        Files.writeString(configs.resolve("offline-euphoria-pipeline.mixins.json"),"""
          {"required":true,"package":"com.euphoriapatches.euphoria_patcher.fabric.mixin","compatibilityLevel":"JAVA_17","client":["IrisModernPipelineManagerMixin"],"refmap":"EuphoriaPatcher-refmap.json","injectors":{"defaultRequire":1}}
          """);
        var urls=new ArrayList<URL>();urls.add(configs.toUri().toURL());
        for(String path:System.getProperty("java.class.path").split(File.pathSeparator))if(!path.isBlank())urls.add(Path.of(path).toUri().toURL());
        URLClassLoader resources=new URLClassLoader(urls.toArray(URL[]::new),VerifyShaderTransforms.class.getClassLoader());
        var f=VerifyMixinTransforms.class.getDeclaredField("LOADER");f.setAccessible(true);f.set(null,resources);
        MixinBootstrap.init();Method phase=MixinEnvironment.class.getDeclaredMethod("gotoPhase",MixinEnvironment.Phase.class);phase.setAccessible(true);
        phase.invoke(null,MixinEnvironment.Phase.INIT);MixinExtrasBootstrap.init();MixinEnvironment.getCurrentEnvironment().setObfuscationContext("named:intermediary");
        Mixins.addConfiguration("offline-euphoria-pipeline.mixins.json");Mixins.addConfiguration("offline-shader-cache.mixins.json");
        for(Config c:Mixins.getConfigs()){c.getConfig().decorate(FabricUtil.KEY_MOD_ID,"prominence_korean_compat");c.getConfig().decorate(FabricUtil.KEY_COMPATIBILITY,FabricUtil.COMPATIBILITY_LATEST);}
        phase.invoke(null,MixinEnvironment.Phase.DEFAULT);MixinEnvironment.getCurrentEnvironment().setObfuscationContext("named:intermediary");
        f=VerifyMixinTransforms.class.getDeclaredField("TRANSFORMER");f.setAccessible(true);var transformer=(IMixinTransformer)f.get(null);
        var defs=new HashMap<String,byte[]>();int methods=0,bridges=0;boolean observed=false,euphoria=false;
        for(String target:List.of("net.irisshaders.iris.shaderpack.preprocessor.JcppProcessor","net.irisshaders.iris.pipeline.PipelineManager")){
            byte[] raw;try(var in=resources.getResourceAsStream(target.replace('.','/')+".class")){raw=in.readAllBytes();}
            byte[] fixed=transformer.transformClassBytes(target,target,raw);if(Arrays.equals(raw,fixed))throw new AssertionError("Missing native transform: "+target);
            ClassNode node=new ClassNode();new ClassReader(fixed).accept(node,0);
            for(var m:node.methods){new Analyzer<BasicValue>(new BasicVerifier()).analyze(node.name,m);methods++;
                for(var i:m.instructions)if(i instanceof MethodInsnNode call){
                    if(call.owner.equals("local/prominence/memory/ShaderPreprocessBridge")&&call.name.equals("preprocess")){
                        // Only inject the test cache directory; production bridge/operations/listener code runs as built.
                        call.owner="local/prominence/memory/ShaderSourceCacheTests";call.name="bridgeNative";bridges++;
                    }
                    if(call.owner.equals("local/prominence/memory/ShaderPreprocessBridge")&&call.name.equals("observe"))observed=true;
                    if(call.owner.contains("DimensionShaderRefresh")&&call.name.equals("beforePreparePipeline"))euphoria=true;
                }
            }
            var writer=new ClassWriter(0);node.accept(writer);defs.put(target,writer.toByteArray());Files.write(work.resolve(target+".class"),fixed);
        }
        if(bridges!=1||!observed||!euphoria)throw new AssertionError("Native bridge/listener/Euphoria hook missing");
        ShaderSourceCacheTests.nativeTests(new NativeLoader(defs).loadClass("net.irisshaders.iris.shaderpack.preprocessor.JcppProcessor"),work,Path.of(args[2]));
        Files.writeString(work.resolve("native-mixin-report.json"),"{\"status\":\"passed\",\"targets\":2,\"methodsBytecodeVerified\":"+methods+",\"euphoriaHookPreserved\":true,\"nativeListenerObserved\":true,\"gameStarted\":false}");
        System.out.println("PASS: native shader Mixin transforms, "+methods+" methods; Euphoria pipeline hook preserved");
    }
}
