package cinderverification;
import batchverification.VerifyMixinTransforms;
import com.llamalad7.mixinextras.MixinExtrasBootstrap;
import java.io.*;import java.lang.reflect.*;import java.net.*;import java.nio.file.*;import java.util.*;
import org.spongepowered.asm.launch.MixinBootstrap;import org.spongepowered.asm.mixin.*;import org.spongepowered.asm.mixin.transformer.*;
import org.objectweb.asm.*;import org.objectweb.asm.tree.*;import org.objectweb.asm.tree.analysis.*;
public class VerifyCinder {
 public static void main(String[]args)throws Exception {
 Path work=Path.of(args[0]);Files.createDirectories(work);Path configs=work;
        var urls=new ArrayList<URL>();urls.add(configs.toUri().toURL());for(String p:System.getProperty("java.class.path").split(File.pathSeparator))if(!p.isBlank())urls.add(Path.of(p).toUri().toURL());
        var resources=new URLClassLoader(urls.toArray(URL[]::new),VerifyCinder.class.getClassLoader());var f=VerifyMixinTransforms.class.getDeclaredField("LOADER");f.setAccessible(true);f.set(null,resources);
        MixinBootstrap.init();Method phase=MixinEnvironment.class.getDeclaredMethod("gotoPhase",MixinEnvironment.Phase.class);phase.setAccessible(true);phase.invoke(null,MixinEnvironment.Phase.INIT);MixinExtrasBootstrap.init();MixinEnvironment.getCurrentEnvironment().setSide(MixinEnvironment.Side.CLIENT);MixinEnvironment.getCurrentEnvironment().setObfuscationContext("named:intermediary");
        Mixins.addConfiguration("prominence-cinder.mixins.json");Mixins.addConfiguration("prominence-cinder.client.mixins.json");
        for(Config c:Mixins.getConfigs()){c.getConfig().decorate(FabricUtil.KEY_MOD_ID,"prominence_korean_compat");c.getConfig().decorate(FabricUtil.KEY_COMPATIBILITY,FabricUtil.COMPATIBILITY_LATEST);}
        phase.invoke(null,MixinEnvironment.Phase.DEFAULT);MixinEnvironment.getCurrentEnvironment().setObfuscationContext("named:intermediary");f=VerifyMixinTransforms.class.getDeclaredField("TRANSFORMER");f.setAccessible(true);var transformer=(IMixinTransformer)f.get(null);

 for(String target:List.of("elocindev.prominentbase.mythicchallenge.MythicChallenge","elocindev.prominentbase.client.gui.ChallengeEndScreen")){
  byte[] raw;try(var in=resources.getResourceAsStream(target.replace('.','/')+".class")){raw=in.readAllBytes();}
  byte[] fixed=transformer.transformClassBytes(target,target,raw);if(Arrays.equals(raw,fixed))throw new AssertionError("Missing transform "+target);
  var node=new ClassNode();new ClassReader(fixed).accept(node,0);
  for(var m:node.methods)new Analyzer<BasicValue>(new BasicVerifier()).analyze(node.name,m);
  System.out.println("PASS native cinder transform: "+target);
 }
 if(local.prominence.cinder.CinderMath.scale(2,4)!=8||local.prominence.cinder.CinderMath.scale(2,1)!=2||local.prominence.cinder.CinderMath.scale(0,4)!=0)throw new AssertionError();
 if(local.prominence.cinder.CinderMath.scale(Integer.MAX_VALUE,1000)!=Integer.MAX_VALUE)throw new AssertionError();
 for(int invalid:new int[]{0,-1,1001}){try{local.prominence.cinder.CinderMath.scale(2,invalid);throw new AssertionError();}catch(IllegalArgumentException expected){}}
 System.out.println("PASS cinder multiplier, identity, zero, overflow and invalid multiplier checks");
 }
}

