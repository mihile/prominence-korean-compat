package rewardverification;
import batchverification.VerifyMixinTransforms;
import com.llamalad7.mixinextras.MixinExtrasBootstrap;
import java.io.*;import java.net.*;import java.nio.file.*;import java.util.*;
import org.objectweb.asm.*;import org.objectweb.asm.tree.*;import org.objectweb.asm.tree.analysis.*;
import org.spongepowered.asm.launch.MixinBootstrap;import org.spongepowered.asm.mixin.*;import org.spongepowered.asm.mixin.transformer.*;
public final class VerifyRewards {
 static byte[] read(ClassLoader l,String n)throws Exception{try(var in=l.getResourceAsStream(n.replace('.','/')+".class")){return in.readAllBytes();}}
 public static void main(String[] a)throws Exception{
  Path cfg=Path.of(a[0]);Files.createDirectories(cfg);
  var gson=new com.google.gson.Gson();
  for(String file:(a[1].equals("server")?List.of("prominence-reward-scaling.mixins.json"):List.of("prominence-reward-scaling.mixins.json","prominence-reward-display.client.mixins.json"))){
   var json=com.google.gson.JsonParser.parseString(new String(readResource(file))).getAsJsonObject();json.remove("plugin");Files.writeString(cfg.resolve("offline-"+file),gson.toJson(json));
  }
  var access=List.of("LootPoolAccessor","LootPoolEntryAccessor","ItemEntryAccessor","LeafEntryAccessor","BinomialLootNumberProviderAccessor","ConstantLootNumberProviderAccessor","UniformLootNumberProviderAccessor","RandomChanceLootConditionAccessor","SetCountLootFunctionAccessor","LootTableMixin");
  Files.writeString(cfg.resolve("offline-access.mixins.json"),gson.toJson(Map.of("required",true,"package","fzzyhmstrs.emi_loot.mixins","compatibilityLevel","JAVA_17","refmap","emi_loot-xplat-refmap.json","mixins",access,"injectors",Map.of("defaultRequire",1))));
  var urls=new ArrayList<URL>();urls.add(cfg.toUri().toURL());for(String p:System.getProperty("java.class.path").split(File.pathSeparator))urls.add(Path.of(p).toUri().toURL());
  var loader=new URLClassLoader(urls.toArray(URL[]::new),VerifyRewards.class.getClassLoader());
  var f=VerifyMixinTransforms.class.getDeclaredField("LOADER");f.setAccessible(true);f.set(null,loader);
  MixinBootstrap.init();var phase=MixinEnvironment.class.getDeclaredMethod("gotoPhase",MixinEnvironment.Phase.class);phase.setAccessible(true);phase.invoke(null,MixinEnvironment.Phase.INIT);MixinExtrasBootstrap.init();
  MixinEnvironment.getCurrentEnvironment().setObfuscationContext("named:intermediary");
  Mixins.addConfiguration("offline-access.mixins.json");Mixins.addConfiguration("offline-prominence-reward-scaling.mixins.json");
  if(!a[1].equals("server"))Mixins.addConfiguration("offline-prominence-reward-display.client.mixins.json");
  for(Config c:Mixins.getConfigs()){c.getConfig().decorate(FabricUtil.KEY_MOD_ID,"prominence_korean_compat");c.getConfig().decorate(FabricUtil.KEY_COMPATIBILITY,FabricUtil.COMPATIBILITY_LATEST);}
  phase.invoke(null,MixinEnvironment.Phase.DEFAULT);MixinEnvironment.getCurrentEnvironment().setObfuscationContext("named:intermediary");
  f=VerifyMixinTransforms.class.getDeclaredField("TRANSFORMER");f.setAccessible(true);var transformer=(IMixinTransformer)f.get(null);
  var targets=new ArrayList<>(List.of("net.minecraft.class_55","net.minecraft.class_85","net.minecraft.class_219","net.minecraft.class_40","net.minecraft.class_85$class_88","net.minecraft.class_77","net.minecraft.class_79","net.minecraft.class_44","net.minecraft.class_5662","net.minecraft.class_141","net.minecraft.class_52","fzzyhmstrs.emi_loot.parser.LootTableParser"));
  if(!a[1].equals("server"))targets.add("dev.emi.emi.api.widget.SlotWidget");
  int hooks=0;for(String target:targets){byte[] bytes=transformer.transformClassBytes(target,target,read(loader,target));var n=new ClassNode();new ClassReader(bytes).accept(n,0);int local=0;
   var original=new ClassNode();new ClassReader(read(loader,target)).accept(original,0);
   if(target.equals("net.minecraft.class_219")||target.equals("net.minecraft.class_40")){long before=randomCalls(original),after=randomCalls(n);if(before!=after||before<1)throw new AssertionError("native RNG count changed "+target);}
   for(var m:n.methods){new Analyzer<BasicValue>(new BasicVerifier()).analyze(n.name,m);for(var ins:m.instructions)if(ins instanceof MethodInsnNode x&&x.owner.startsWith("local/prominence/rewards/")){local++;hooks++;}}
   if(List.of("net.minecraft.class_55","net.minecraft.class_219","net.minecraft.class_40","net.minecraft.class_85$class_88","fzzyhmstrs.emi_loot.parser.LootTableParser","dev.emi.emi.api.widget.SlotWidget").contains(target)&&local==0)throw new AssertionError("missing reward injection: "+target);
   if(target.equals("net.minecraft.class_85")&&!n.interfaces.contains("local/prominence/rewards/RewardLeaf"))throw new AssertionError("quality accessor missing");
   System.out.println("PASS actual "+a[1]+" transform + ASM verification "+target+" hooks="+local);
  }if(hooks<8)throw new AssertionError("missing hooks");System.out.println("PASS native transforms completed; no game/server/world started");
 }
 static long randomCalls(ClassNode n){long count=0;for(var m:n.methods)for(var i:m.instructions)if(i instanceof MethodInsnNode x&&x.owner.equals("net/minecraft/class_5819"))count++;return count;}
 static byte[] readResource(String n)throws Exception{try(var in=VerifyRewards.class.getClassLoader().getResourceAsStream(n)){return in.readAllBytes();}}
}
