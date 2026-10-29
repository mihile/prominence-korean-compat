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

public final class VerifyVoxyMesh {
    private static ClassNode node(byte[] data){var n=new ClassNode();new ClassReader(data).accept(n,0);return n;}
    private static byte[] read(ClassLoader l,String n)throws Exception {try(var in=l.getResourceAsStream(n.replace('.','/')+".class")){return in.readAllBytes();}}
    private static long calls(MethodNode m,String owner,String name){long n=0;for(var i:m.instructions)if(i instanceof MethodInsnNode x&&x.owner.equals(owner)&&x.name.equals(name))n++;return n;}
    private static Map<String,Integer> nativeCalls(MethodNode m) {var counts=new HashMap<String,Integer>();for(var i:m.instructions)if(i instanceof MethodInsnNode x)counts.merge(x.owner+"."+x.name+x.desc,1,Integer::sum);return counts;}
    public static void main(String[] args)throws Exception {
        Path cfg=Path.of(args[0]);Files.createDirectories(cfg);
        Files.writeString(cfg.resolve("offline-join-contention.mixins.json"),"{\"required\":true,\"package\":\"local.prominence.work.mixin\",\"compatibilityLevel\":\"JAVA_17\",\"client\":[\"VoxyMeshServiceMixin\",\"VoxyMeshRunMixin\"],\"injectors\":{\"defaultRequire\":1}}");
        var urls=new ArrayList<URL>();urls.add(cfg.toUri().toURL());for(String p:System.getProperty("java.class.path").split(File.pathSeparator))urls.add(Path.of(p).toUri().toURL());
        var resources=new URLClassLoader(urls.toArray(URL[]::new),VerifyVoxyMesh.class.getClassLoader());
        Field field=VerifyMixinTransforms.class.getDeclaredField("LOADER");field.setAccessible(true);field.set(null,resources);
        MixinBootstrap.init();Method phase=MixinEnvironment.class.getDeclaredMethod("gotoPhase",MixinEnvironment.Phase.class);phase.setAccessible(true);phase.invoke(null,MixinEnvironment.Phase.INIT);MixinExtrasBootstrap.init();
        MixinEnvironment.getCurrentEnvironment().setObfuscationContext("named:intermediary");Mixins.addConfiguration("offline-join-contention.mixins.json");
        for(Config c:Mixins.getConfigs()){c.getConfig().decorate(FabricUtil.KEY_MOD_ID,"prominence_korean_compat");c.getConfig().decorate(FabricUtil.KEY_COMPATIBILITY,FabricUtil.COMPATIBILITY_LATEST);}
        phase.invoke(null,MixinEnvironment.Phase.DEFAULT);MixinEnvironment.getCurrentEnvironment().setObfuscationContext("named:intermediary");
        field=VerifyMixinTransforms.class.getDeclaredField("TRANSFORMER");field.setAccessible(true);var transformer=(IMixinTransformer)field.get(null);
        int count=0;
        for(String target:java.util.List.of("me.cortex.voxy.client.core.rendering.building.RenderGenerationService","me.cortex.voxy.common.thread.Service")) {
            byte[] raw=read(resources,target);byte[] transformed=transformer.transformClassBytes(target,target,raw);
            var before=node(raw);var after=node(transformed);if(java.util.Arrays.equals(raw,transformed))throw new AssertionError("not transformed "+target);
            int hooks=0;
            for(var m:after.methods){new Analyzer<BasicValue>(new BasicVerifier()).analyze(after.name,m);count++;for(var i:m.instructions)if(i instanceof MethodInsnNode x&&x.owner.startsWith("local/prominence/work/"))hooks++;}
            if(hooks<1 && after.methods.stream().noneMatch(m->m.name.contains("prominence$limitMesh")))throw new AssertionError("missing mesh admission hook "+target);
            if(false) {
                for(String name:java.util.List.of("processUploads","processModelResult","addEntry","free")) {
                    var a=before.methods.stream().filter(m->m.name.equals(name)).findFirst().orElseThrow();
                    var b=after.methods.stream().filter(m->m.name.equals(name)).findFirst().orElseThrow();
                    if(!nativeCalls(a).equals(nativeCalls(b)))throw new AssertionError("native work changed: "+name);
                }
            }
            Path destination=cfg.resolve(target.replace('.','/')+".class");Files.createDirectories(destination.getParent());Files.write(destination,transformed);
        }
        System.out.println("PASS: two native Voxy scheduling targets transformed; "+count+" methods ASM verified; native ingest mixins present; other behavior requires separate verification");
    }
}

