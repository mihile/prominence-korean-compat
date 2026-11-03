package infusionverification;
import batchverification.VerifyMixinTransforms;
import com.llamalad7.mixinextras.MixinExtrasBootstrap;
import java.io.File;
import java.lang.reflect.Method;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.transformer.*;

/** Verify native Eldritch smithing coexistence and replacement order without a game. */
public final class VerifyInfusionTransforms {
 public static void main(String[] args)throws Exception {
  Path root=Path.of(args[0]),work=Path.of(args[1]);Files.createDirectories(work);
  Files.writeString(work.resolve("offline-infusion.mixins.json"),Files.readString(root.resolve("prominence-infusion.mixins.json")).replace("local.prominence.infusion.InfusionGuard",VerifyMixinTransforms.OfflinePlugin.class.getName()));
  Files.writeString(work.resolve("offline-eldritch.mixins.json"),"{\"required\":true,\"package\":\"elocindev.eldritch_end.mixin\",\"compatibilityLevel\":\"JAVA_17\",\"mixins\":[\"smithing.InfusionSmithingTableMixin\",\"smithing.ForgingScreenHandlerAccessor\"],\"refmap\":\"Eldritch_End-LOADER-MC1.20.1-refmap.json\",\"injectors\":{\"defaultRequire\":1}}");
  var urls=new ArrayList<URL>();urls.add(work.toUri().toURL());for(String p:System.getProperty("java.class.path").split(File.pathSeparator))if(!p.isBlank())urls.add(Path.of(p).toUri().toURL());
  var resources=new URLClassLoader(urls.toArray(URL[]::new),VerifyInfusionTransforms.class.getClassLoader());var f=VerifyMixinTransforms.class.getDeclaredField("LOADER");f.setAccessible(true);f.set(null,resources);
  MixinBootstrap.init();Method phase=MixinEnvironment.class.getDeclaredMethod("gotoPhase",MixinEnvironment.Phase.class);phase.setAccessible(true);phase.invoke(null,MixinEnvironment.Phase.INIT);
  MixinExtrasBootstrap.init();MixinEnvironment.getCurrentEnvironment().setSide(MixinEnvironment.Side.valueOf(args.length>2?args[2]:"CLIENT"));MixinEnvironment.getCurrentEnvironment().setObfuscationContext("named:intermediary");Mixins.addConfiguration("offline-eldritch.mixins.json");Mixins.addConfiguration("offline-infusion.mixins.json");
  for(Config c:Mixins.getConfigs()){c.getConfig().decorate(FabricUtil.KEY_MOD_ID,"prominence_korean_compat");c.getConfig().decorate(FabricUtil.KEY_COMPATIBILITY,FabricUtil.COMPATIBILITY_LATEST);}
  phase.invoke(null,MixinEnvironment.Phase.DEFAULT);MixinEnvironment.getCurrentEnvironment().setObfuscationContext("named:intermediary");f=VerifyMixinTransforms.class.getDeclaredField("TRANSFORMER");f.setAccessible(true);var transformer=(IMixinTransformer)f.get(null);
  int methods=0;
  for(String target:List.of("net.minecraft.class_4861","net.minecraft.class_4862")) {
   byte[] raw;try(var in=resources.getResourceAsStream(target.replace('.','/')+".class")){raw=in.readAllBytes();}
   byte[] fixed=transformer.transformClassBytes(target,target,raw);if(Arrays.equals(raw,fixed))throw new AssertionError("Untransformed "+target);
   var node=new ClassNode();new ClassReader(fixed).accept(node,0);
   for(var method:node.methods){new Analyzer<BasicValue>(new BasicVerifier()).analyze(node.name,method);methods++;}
   if(target.endsWith("4862")) {
    var update=node.methods.stream().filter(m->m.name.equals("method_24928")).findFirst().orElseThrow();
    int custom=-1,nativeCall=-1,i=0;
    for(var insn:update.instructions){if(insn instanceof MethodInsnNode call) {
     if(call.name.contains("prominence$replaceInfusion"))custom=i;
     if(call.name.contains("eldritch_end$updateResult"))nativeCall=i;
    }i++;}
    if(custom<0||nativeCall<0||custom>=nativeCall){Files.write(work.resolve("failed-smithing.class"),fixed);throw new AssertionError("Replacement must run before native append-only infusion: custom="+custom+", native="+nativeCall);}
    var take=node.methods.stream().filter(m->m.name.equals("method_24923")).findFirst().orElseThrow();
    var original=new ClassNode();new ClassReader(raw).accept(original,0);
    var originalTake=original.methods.stream().filter(m->m.name.equals("method_24923")).findFirst().orElseThrow();
    if(take.instructions.size()!=originalTake.instructions.size())throw new AssertionError("Material consumption modified");
   }
   Files.write(work.resolve(target.substring(target.lastIndexOf('.')+1)+".class"),fixed);
  }
  Files.writeString(work.resolve("report.json"),"{\"targets\":2,\"verifiedMethods\":"+methods+",\"nativeCoexistence\":true,\"gameStarted\":false}");
  System.out.println("Native infusion transforms passed: 2 targets, "+methods+" methods; native consumption preserved");
 }
}
