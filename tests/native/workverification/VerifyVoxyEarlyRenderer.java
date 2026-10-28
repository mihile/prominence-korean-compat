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

public final class VerifyVoxyEarlyRenderer {
    private static ClassNode node(byte[] data){var n=new ClassNode();new ClassReader(data).accept(n,0);return n;}
    private static byte[] read(ClassLoader l,String n)throws Exception {try(var in=l.getResourceAsStream(n.replace('.','/')+".class")){return in.readAllBytes();}}
    private static long calls(MethodNode m,String owner,String name){long n=0;for(var i:m.instructions)if(i instanceof MethodInsnNode x&&x.owner.equals(owner)&&x.name.equals(name))n++;return n;}
    private static Map<String,Integer> nativeCalls(MethodNode m) {var counts=new HashMap<String,Integer>();for(var i:m.instructions)if(i instanceof MethodInsnNode x)counts.merge(x.owner+"."+x.name+x.desc,1,Integer::sum);return counts;}
    public static void main(String[] args)throws Exception {
        Path cfg=Path.of(args[0]);Files.createDirectories(cfg);
        Files.writeString(cfg.resolve("offline-voxy-render.mixins.json"),"{\"required\":true,\"package\":\"local.prominence.work.mixin\",\"compatibilityLevel\":\"JAVA_17\",\"client\":[\"VoxyServerEarlyRendererMixin\",\"VoxyScreenUvMixin\"],\"injectors\":{\"defaultRequire\":1}}");
        var urls=new ArrayList<URL>();urls.add(cfg.toUri().toURL());for(String p:System.getProperty("java.class.path").split(File.pathSeparator))urls.add(Path.of(p).toUri().toURL());
        var resources=new URLClassLoader(urls.toArray(URL[]::new),VerifyVoxyEarlyRenderer.class.getClassLoader());
        Field field=VerifyMixinTransforms.class.getDeclaredField("LOADER");field.setAccessible(true);field.set(null,resources);
        MixinBootstrap.init();Method phase=MixinEnvironment.class.getDeclaredMethod("gotoPhase",MixinEnvironment.Phase.class);phase.setAccessible(true);phase.invoke(null,MixinEnvironment.Phase.INIT);MixinExtrasBootstrap.init();
        MixinEnvironment.getCurrentEnvironment().setObfuscationContext("named:intermediary");Mixins.addConfiguration("offline-voxy-render.mixins.json");
        for(Config c:Mixins.getConfigs()){c.getConfig().decorate(FabricUtil.KEY_MOD_ID,"prominence_korean_compat");c.getConfig().decorate(FabricUtil.KEY_COMPATIBILITY,FabricUtil.COMPATIBILITY_LATEST);}
        phase.invoke(null,MixinEnvironment.Phase.DEFAULT);MixinEnvironment.getCurrentEnvironment().setObfuscationContext("named:intermediary");
        field=VerifyMixinTransforms.class.getDeclaredField("TRANSFORMER");field.setAccessible(true);var transformer=(IMixinTransformer)field.get(null);
        String target="com.dripps.voxyserver.client.ClientLodReceiver";var before=node(read(resources,target));var tick=before.methods.stream().filter(m->m.name.equals("tick")).findFirst().orElseThrow();
        String iface="me/cortex/voxy/client/core/IGetVoxyRenderSystem";
        if(calls(tick,iface,"voxy$createRenderer")!=1)throw new AssertionError("native target call mismatch");
        var after=node(transformer.transformClassBytes(target,target,read(resources,target)));int count=0;boolean gate=false,iris=false;
        for(var m:after.methods){new Analyzer<BasicValue>(new BasicVerifier()).analyze(after.name,m);count++;for(var i:m.instructions)if(i instanceof MethodInsnNode x){gate|=x.owner.equals("local/prominence/work/EarlyRendererGate")&&x.name.equals("invoke");iris|=x.owner.equals("me/cortex/voxy/client/core/util/IrisUtil")&&x.name.equals("irisShaderPackEnabled");}}
        if(!gate||!iris)throw new AssertionError("transformed shader gate missing");
        var transformedTick=after.methods.stream().filter(m->m.name.equals("tick")).findFirst().orElseThrow();
        if(calls(transformedTick,iface,"voxy$createRenderer")!=0)throw new AssertionError("early native call not wrapped");
        var originalCalls=nativeCalls(tick);var newCalls=nativeCalls(transformedTick);
        for(var call:originalCalls.entrySet())if(!call.getKey().equals(iface+".voxy$createRenderer()V")&&!call.getValue().equals(newCalls.get(call.getKey())))throw new AssertionError("other native tick call changed: "+call);
        var oldDrain=before.methods.stream().filter(m->m.name.equals("drain")).findFirst().orElseThrow();var newDrain=after.methods.stream().filter(m->m.name.equals("drain")).findFirst().orElseThrow();
        if(!nativeCalls(oldDrain).equals(nativeCalls(newDrain)))throw new AssertionError("remote ingest drain changed");
        var irisClass=node(read(resources,"net.irisshaders.iris.pipeline.IrisRenderingPipeline"));var begin=irisClass.methods.stream().filter(m->m.name.equals("beginLevelRendering")).findFirst().orElseThrow();int index=0,reload=-1,active=-1;
        for(var i:begin.instructions){if(i instanceof MethodInsnNode x){if(x.owner.equals("net/minecraft/class_761")&&x.name.equals("method_3279"))reload=index;if(x.owner.equals("com/mojang/blaze3d/systems/RenderSystem")&&x.name.equals("activeTexture")&&active<0)active=index;}index++;}
        if(reload<0||active<=reload)throw new AssertionError("Iris block-ID reload must precede Voxy frame hook");
        String shaderTarget="me.cortex.voxy.client.iris.IrisShaderPatch";
        var shaderAfter=node(transformer.transformClassBytes(shaderTarget,shaderTarget,read(resources,shaderTarget)));
        boolean uvCall=false;
        for(var method:shaderAfter.methods){new Analyzer<BasicValue>(new BasicVerifier()).analyze(shaderAfter.name,method);
            for(var instruction:method.instructions)if(instruction instanceof MethodInsnNode call&&call.owner.equals("local/prominence/work/VoxyScreenUvFix")&&call.name.equals("fix"))uvCall=true;
        }
        if(!uvCall)throw new AssertionError("Native Voxy screen UV wrapper missing");
        System.out.println("PASS: actual Voxy IrisShaderPatch wrapped and ASM-verified");
        System.out.println("PASS: actual VoxyServer tick transformed; "+count+" methods verified; other native tick calls and ingest drain unchanged; Iris reload precedes Voxy frame hook; no game/GL");
    }
}
