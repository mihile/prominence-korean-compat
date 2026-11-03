package retentionverification;
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

/** Transform real native constructors, including final previous-screen field, without a game. */
public final class VerifyRetentionTransforms {
 static boolean tracked(ClassNode owner,MethodNode method,Set<String> seen) {
  if(!seen.add(method.desc))return false;
  for(var insn:method.instructions)if(insn instanceof MethodInsnNode call) {
   if(call.name.contains("prominence$track"))return true;
   if(call.owner.equals(owner.name)&&call.name.equals("<init>")) {
    var next=owner.methods.stream().filter(m->m.name.equals("<init>")&&m.desc.equals(call.desc)).findFirst();
    if(next.isPresent()&&tracked(owner,next.get(),seen))return true;
   }
  }
  return false;
 }
 public static void main(String[] args)throws Exception {
  Path root=Path.of(args[0]),work=Path.of(args[1]);Files.createDirectories(work);
  Files.writeString(work.resolve("offline-retention.mixins.json"),Files.readString(root.resolve("prominence-retention.client.mixins.json")).replace("local.prominence.memory.RetentionMixinGuard",VerifyMixinTransforms.OfflinePlugin.class.getName()));
  Files.writeString(work.resolve("offline-existing-hud.mixins.json"),"{\"required\":true,\"package\":\"local.prominence.trinketalloc.mixin\",\"compatibilityLevel\":\"JAVA_17\",\"client\":[\"SpellHudScopeMixin\"],\"injectors\":{\"defaultRequire\":1}}");
  Files.writeString(work.resolve("offline-weapon-hud.mixins.json"),"{\"required\":true,\"package\":\"local.prominence.weaponhud.mixin\",\"compatibilityLevel\":\"JAVA_17\",\"client\":[\"WeaponHudMixin\"],\"injectors\":{\"defaultRequire\":1}}");
  var urls=new ArrayList<URL>();urls.add(work.toUri().toURL());for(String p:System.getProperty("java.class.path").split(File.pathSeparator))if(!p.isBlank())urls.add(Path.of(p).toUri().toURL());
  var resources=new URLClassLoader(urls.toArray(URL[]::new),VerifyRetentionTransforms.class.getClassLoader());var f=VerifyMixinTransforms.class.getDeclaredField("LOADER");f.setAccessible(true);f.set(null,resources);
  MixinBootstrap.init();Method phase=MixinEnvironment.class.getDeclaredMethod("gotoPhase",MixinEnvironment.Phase.class);phase.setAccessible(true);phase.invoke(null,MixinEnvironment.Phase.INIT);
  MixinExtrasBootstrap.init();MixinEnvironment.getCurrentEnvironment().setSide(MixinEnvironment.Side.CLIENT);MixinEnvironment.getCurrentEnvironment().setObfuscationContext("named:intermediary");Mixins.addConfiguration("offline-existing-hud.mixins.json");Mixins.addConfiguration("offline-weapon-hud.mixins.json");Mixins.addConfiguration("offline-retention.mixins.json");
  for(Config c:Mixins.getConfigs()){c.getConfig().decorate(FabricUtil.KEY_MOD_ID,"prominence_korean_compat");c.getConfig().decorate(FabricUtil.KEY_COMPATIBILITY,FabricUtil.COMPATIBILITY_LATEST);}
  phase.invoke(null,MixinEnvironment.Phase.DEFAULT);MixinEnvironment.getCurrentEnvironment().setObfuscationContext("named:intermediary");f=VerifyMixinTransforms.class.getDeclaredField("TRANSFORMER");f.setAccessible(true);var transformer=(IMixinTransformer)f.get(null);
  int methods=0,constructors=0;
  for(String target:List.of("mod.azure.azurelib.rewrite.render.AzRendererPipelineContext","dev.ftb.mods.ftblibrary.ui.BaseScreen","mod.azure.azurelib.renderer.GeoArmorRenderer","mod.azure.azurelibarmor.renderer.GeoArmorRenderer","net.spell_engine.client.gui.HudRenderHelper","net.spell_engine.internals.SpellContainerHelper")) {
   byte[] raw;try(var in=resources.getResourceAsStream(target.replace('.','/')+".class")){raw=in.readAllBytes();}
   byte[] fixed=transformer.transformClassBytes(target,target,raw);if(Arrays.equals(raw,fixed))throw new AssertionError("Untransformed "+target);
   var node=new ClassNode();new ClassReader(fixed).accept(node,0);
   boolean owner=target.contains("AzRendererPipelineContext")||target.endsWith("BaseScreen")||target.endsWith("GeoArmorRenderer");
   if(owner&&!node.interfaces.contains("local/prominence/memory/RetainedClientOwner"))throw new AssertionError("Missing owner interface");
   for(var method:node.methods){new Analyzer<BasicValue>(new BasicVerifier()).analyze(node.name,method);methods++;if(owner&&method.name.equals("<init>")) {
    if(!tracked(node,method,new HashSet<>()))throw new AssertionError("Constructor not tracked "+method.desc);constructors++;
   }}
   if(target.endsWith("BaseScreen")&&node.fields.stream().filter(v->v.name.equals("prevScreen")).anyMatch(v->(v.access&Opcodes.ACC_FINAL)!=0))throw new AssertionError("Previous screen still final");
   if(target.endsWith("GeoArmorRenderer")) {
    var prep=node.methods.stream().filter(m->m.name.equals("prepForRender")).findFirst().orElseThrow();var fields=new HashSet<String>();
    for(var insn:prep.instructions)if(insn instanceof FieldInsnNode field&&field.getOpcode()==Opcodes.PUTFIELD)fields.add(field.name);
    if(!fields.containsAll(List.of("currentEntity","currentStack","currentSlot","baseModel")))throw new AssertionError("Armor state not repopulated before render");
   }
   if(target.endsWith("HudRenderHelper")&&node.methods.stream().noneMatch(m->m.name.contains("prominence$spellFrame")))throw new AssertionError("Missing HUD scope");
   if(target.endsWith("SpellContainerHelper")&&node.methods.stream().noneMatch(m->m.name.contains("prominence$readEquipped")))throw new AssertionError("Missing equipped-spell read hook");
   Files.write(work.resolve(target.substring(target.lastIndexOf('.')+1)+".class"),fixed);
  }
  Files.writeString(work.resolve("report.json"),"{\"targets\":6,\"verifiedMethods\":"+methods+",\"trackedConstructors\":"+constructors+",\"gameStarted\":false}");
  System.out.println("Native retention transforms passed: 6 targets, "+methods+" methods, "+constructors+" constructors");
 }
}
