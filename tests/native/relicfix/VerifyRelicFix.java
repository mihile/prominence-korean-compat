package workverification;

import batchverification.VerifyMixinTransforms;
import com.llamalad7.mixinextras.MixinExtrasBootstrap;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.transformer.*;

public final class VerifyRelicFix {
    private static ClassNode node(byte[] data){var n=new ClassNode();new ClassReader(data).accept(n,0);return n;}
    private static byte[] read(ClassLoader l,String n)throws Exception {try(var in=l.getResourceAsStream(n.replace('.','/')+".class")){return in.readAllBytes();}}
    private static long calls(MethodNode m,String owner,String name){long n=0;for(var i:m.instructions)if(i instanceof MethodInsnNode x&&x.owner.equals(owner)&&x.name.equals(name))n++;return n;}
    private static Map<String,Integer> nativeCalls(MethodNode m) {var counts=new HashMap<String,Integer>();for(var i:m.instructions)if(i instanceof MethodInsnNode x)counts.merge(x.owner+"."+x.name+x.desc,1,Integer::sum);return counts;}
    public static void main(String[] args)throws Exception {
        Path cfg=Path.of(args[0]);Files.createDirectories(cfg);
        Files.writeString(cfg.resolve("offline-join-contention.mixins.json"),"{\"required\":true,\"package\":\"local.prominence.relicfix.mixin\",\"compatibilityLevel\":\"JAVA_17\",\"client\":[\"RelicRecoveryMessageMixin\"],\"injectors\":{\"defaultRequire\":1}}");
        var urls=new ArrayList<URL>();urls.add(cfg.toUri().toURL());for(String p:System.getProperty("java.class.path").split(File.pathSeparator))urls.add(Path.of(p).toUri().toURL());
        var resources=new URLClassLoader(urls.toArray(URL[]::new),VerifyRelicFix.class.getClassLoader());
        Field field=VerifyMixinTransforms.class.getDeclaredField("LOADER");field.setAccessible(true);field.set(null,resources);
        MixinBootstrap.init();Method phase=MixinEnvironment.class.getDeclaredMethod("gotoPhase",MixinEnvironment.Phase.class);phase.setAccessible(true);phase.invoke(null,MixinEnvironment.Phase.INIT);MixinExtrasBootstrap.init();
        MixinEnvironment.getCurrentEnvironment().setObfuscationContext("named:intermediary");Mixins.addConfiguration("offline-join-contention.mixins.json");
        for(Config c:Mixins.getConfigs()){c.getConfig().decorate(FabricUtil.KEY_MOD_ID,"prominence_korean_compat");c.getConfig().decorate(FabricUtil.KEY_COMPATIBILITY,FabricUtil.COMPATIBILITY_LATEST);}
        phase.invoke(null,MixinEnvironment.Phase.DEFAULT);MixinEnvironment.getCurrentEnvironment().setObfuscationContext("named:intermediary");
        field=VerifyMixinTransforms.class.getDeclaredField("TRANSFORMER");field.setAccessible(true);var transformer=(IMixinTransformer)field.get(null);
        int count=0;
        String target="elocindev.prominent.recovery.RelicRecoveryManager";
        byte[] raw=read(resources,target);byte[] transformed=transformer.transformClassBytes(target,target,raw);
        var after=node(transformed);if(java.util.Arrays.equals(raw,transformed))throw new AssertionError("not transformed");
        int hooks=0;for(var m:after.methods){new Analyzer<BasicValue>(new BasicVerifier()).analyze(after.name,m);count++;if(m.name.contains("prominence$"))hooks++;}
        if(hooks<2)throw new AssertionError("missing mixin wrappers "+hooks);
        java.nio.file.Files.write(cfg.resolve(target+".class"),transformed);
        System.out.println("PASS: actual relic manager transformed; "+count+" methods ASM verified; two wrappers verified");
    }
}

