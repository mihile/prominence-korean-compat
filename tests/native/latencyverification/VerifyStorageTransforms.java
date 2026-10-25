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
import java.util.List;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.BasicVerifier;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.FabricUtil;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.transformer.Config;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;

public final class VerifyStorageTransforms {
    private static List<String> calls(MethodNode method){var out=new ArrayList<String>();for(var i:method.instructions)if(i instanceof MethodInsnNode c)out.add(c.owner+"."+c.name+c.desc);return out;}
    public static void main(String[] args)throws Exception {
        Path work=Path.of(args[1]);Files.createDirectories(work);Path configs=work.resolve("configs");Files.createDirectories(configs);
        String plugin=VerifyMixinTransforms.OfflinePlugin.class.getName();
        Files.writeString(configs.resolve("offline-storage.mixins.json"),Files.readString(Path.of(args[0])).replace("local.prominence.memory.VoxyStorageGuard",plugin));
        Files.writeString(configs.resolve("offline-voxy-level.mixins.json"),"""
          {"required":true,"package":"me.cortex.voxy.client.mixin","compatibilityLevel":"JAVA_17","client":["minecraft.MixinLevelRenderer"],"refmap":"voxy-refmap.json","injectors":{"defaultRequire":1}}
          """);
        Files.writeString(configs.resolve("offline-voxyserver-instance.mixins.json"),"""
          {"required":true,"package":"com.dripps.voxyserver.client.mixin","compatibilityLevel":"JAVA_17","client":["VoxyInstanceMixin"],"injectors":{"defaultRequire":1}}
          """);
        var urls=new ArrayList<URL>();urls.add(configs.toUri().toURL());for(String path:System.getProperty("java.class.path").split(File.pathSeparator))if(!path.isBlank())urls.add(Path.of(path).toUri().toURL());
        URLClassLoader resources=new URLClassLoader(urls.toArray(URL[]::new),VerifyStorageTransforms.class.getClassLoader());
        var f=VerifyMixinTransforms.class.getDeclaredField("LOADER");f.setAccessible(true);f.set(null,resources);
        MixinBootstrap.init();Method phase=MixinEnvironment.class.getDeclaredMethod("gotoPhase",MixinEnvironment.Phase.class);phase.setAccessible(true);phase.invoke(null,MixinEnvironment.Phase.INIT);MixinExtrasBootstrap.init();
        MixinEnvironment.getCurrentEnvironment().setObfuscationContext("named:intermediary");
        Mixins.addConfiguration("offline-voxy-level.mixins.json");Mixins.addConfiguration("offline-voxyserver-instance.mixins.json");Mixins.addConfiguration("offline-storage.mixins.json");
        for(Config c:Mixins.getConfigs()){c.getConfig().decorate(FabricUtil.KEY_MOD_ID,"prominence_korean_compat");c.getConfig().decorate(FabricUtil.KEY_COMPATIBILITY,FabricUtil.COMPATIBILITY_LATEST);}
        phase.invoke(null,MixinEnvironment.Phase.DEFAULT);MixinEnvironment.getCurrentEnvironment().setObfuscationContext("named:intermediary");
        f=VerifyMixinTransforms.class.getDeclaredField("TRANSFORMER");f.setAccessible(true);var transformer=(IMixinTransformer)f.get(null);
        int verified=0,bridges=0,preserved=0;
        for(String target:List.of("me.cortex.voxy.commonImpl.VoxyInstance","me.cortex.voxy.commonImpl.WorldIdentifier","net.minecraft.class_761")){
            byte[] raw;try(var in=resources.getResourceAsStream(target.replace('.','/')+".class")){raw=in.readAllBytes();}
            byte[] fixed=transformer.transformClassBytes(target,target,raw);if(Arrays.equals(raw,fixed))throw new AssertionError("No transform: "+target);
            ClassNode original=new ClassNode(),node=new ClassNode();new ClassReader(raw).accept(original,0);new ClassReader(fixed).accept(node,0);
            for(var method:node.methods){new Analyzer<BasicValue>(new BasicVerifier()).analyze(node.name,method);verified++;
                for(var i:method.instructions)if(i instanceof MethodInsnNode call&&call.owner.equals("local/prominence/memory/VoxyStorageRecovery"))bridges++;
            }
            if(!target.startsWith("net.minecraft")){
                String methodName=target.endsWith("VoxyInstance")?"getOrCreate":"getOrCreateEngine";
                var nativeMethod=original.methods.stream().filter(m->m.name.equals(methodName)&&(methodName.equals("getOrCreateEngine")||m.desc.contains(";Z)"))).findFirst().orElseThrow();
                var expected=calls(nativeMethod);boolean found=node.methods.stream().anyMatch(m->!m.name.equals(nativeMethod.name)&&calls(m).equals(expected));
                if(!found)throw new AssertionError("Original native method calls/finally not preserved: "+target);preserved++;
            }else if(node.methods.stream().noneMatch(m->m.name.contains("voxy$createRenderer")))throw new AssertionError("Native Voxy renderer not merged");
            Files.write(work.resolve(target+".class"),fixed);
        }
        if(bridges!=3||preserved!=2)throw new AssertionError("Bridge/native body inventory mismatch "+bridges+"/"+preserved);
        Files.writeString(work.resolve("native-mixin-report.json"),"{\"status\":\"passed\",\"targets\":3,\"methodsBytecodeVerified\":"+verified+",\"nativeBodiesPreserved\":"+preserved+",\"bridges\":"+bridges+",\"gameStarted\":false}");
        System.out.println("PASS: native Voxy/VoxyServer/WorldRenderer Mixin transforms: 3 targets, "+verified+" methods, native open/unlock and identifier bodies preserved");
    }
}
