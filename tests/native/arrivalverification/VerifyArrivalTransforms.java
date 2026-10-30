package arrivalverification;


import com.llamalad7.mixinextras.MixinExtrasBootstrap;
import java.net.*;import java.nio.file.*;import java.io.*;import java.lang.reflect.*;import java.util.*;
import org.objectweb.asm.*;import org.objectweb.asm.tree.*;import org.objectweb.asm.tree.analysis.*;
import org.spongepowered.asm.launch.MixinBootstrap;import org.spongepowered.asm.mixin.*;import org.spongepowered.asm.mixin.transformer.*;

/** Transform installed classes and verify hook placement without creating a player or world. */
public final class VerifyArrivalTransforms {
 public static void main(String[] args)throws Exception{
  Class<?> service=Class.forName(args[2]);
  Path out=Path.of(args[1]);Files.createDirectories(out);Path configs=out.resolve("configs");Files.createDirectories(configs);
  Files.writeString(configs.resolve("offline-arrival.mixins.json"),Files.readString(Path.of(args[0])).replace("local.prominence.arrival.ArrivalGuard",service.getName()+"$OfflinePlugin"));
  var urls=new ArrayList<URL>();urls.add(configs.toUri().toURL());for(String s:System.getProperty("java.class.path").split(File.pathSeparator))urls.add(Path.of(s).toUri().toURL());
  var loader=new URLClassLoader(urls.toArray(URL[]::new),VerifyArrivalTransforms.class.getClassLoader());var field=service.getDeclaredField("LOADER");field.setAccessible(true);field.set(null,loader);
  MixinBootstrap.init();Method phase=MixinEnvironment.class.getDeclaredMethod("gotoPhase",MixinEnvironment.Phase.class);phase.setAccessible(true);phase.invoke(null,MixinEnvironment.Phase.INIT);
  MixinExtrasBootstrap.init();MixinEnvironment.getCurrentEnvironment().setObfuscationContext("named:intermediary");Mixins.addConfiguration("offline-arrival.mixins.json");
  for(Config c:Mixins.getConfigs()){c.getConfig().decorate(FabricUtil.KEY_MOD_ID,"prominence_korean_compat");c.getConfig().decorate(FabricUtil.KEY_COMPATIBILITY,FabricUtil.COMPATIBILITY_LATEST);}
  phase.invoke(null,MixinEnvironment.Phase.DEFAULT);MixinEnvironment.getCurrentEnvironment().setObfuscationContext("named:intermediary");
  field=service.getDeclaredField("TRANSFORMER");field.setAccessible(true);var transformer=(IMixinTransformer)field.get(null);
  int methods=0,begin=0,arrived=0,abort=0,damage=0;
  for(String name:List.of("wraith.fwaystones.block.WaystoneBlockEntity","net.minecraft.class_3222")){
   byte[] original;try(var in=loader.getResourceAsStream(name.replace('.','/')+".class")){original=in.readAllBytes();}
   byte[] changed=transformer.transformClassBytes(name,name,original);if(Arrays.equals(original,changed))throw new AssertionError("No arrival transform: "+name);
   ClassNode node=new ClassNode();new ClassReader(changed).accept(node,0);
   for(var m:node.methods){new Analyzer<BasicValue>(new BasicVerifier()).analyze(node.name,m);methods++;
    for(var instruction:m.instructions)if(instruction instanceof MethodInsnNode call&&call.owner.equals("local/prominence/arrival/ArrivalServer")){
     switch(call.name){case "begin"->begin++;case "arrived"->arrived++;case "abort"->abort++;case "protectedNow"->damage++;}
    }
   }
   if(name.equals("net.minecraft.class_3222")){
    var damageMethod=node.methods.stream().filter(m->m.name.equals("method_5643")).findFirst().orElseThrow();
    int firstNativeCall=-1,cancelReturn=-1,index=0;
    for(var instruction:damageMethod.instructions){if(instruction instanceof MethodInsnNode call){if(call.owner.equals("org/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable")&&call.name.equals("getReturnValueZ"))cancelReturn=index;if(call.owner.equals("net/minecraft/class_1282")&&firstNativeCall<0)firstNativeCall=index;}index++;}
    if(cancelReturn<0||firstNativeCall<0||cancelReturn>=firstNativeCall)throw new AssertionError("Damage cancellation is not before native damage processing");
   }
   Files.write(out.resolve(name+".class"),changed);
  }
  if(begin!=1||arrived!=1||abort!=2||damage!=1)throw new AssertionError("Missing teleport success/failure or damage hooks: "+begin+"/"+arrived+"/"+abort+"/"+damage);
  String report="{\"status\":\"passed\",\"targets\":2,\"methods\":"+methods+",\"teleportBeforeSuccessFailureHooks\":true,\"damageCancelledBeforeNativeProcessing\":true,\"side\":\""+MixinEnvironment.getCurrentEnvironment().getSide()+"\",\"gameStarted\":false}";
  Files.writeString(out.resolve("report.json"),report);System.out.println(report);
 }
}
