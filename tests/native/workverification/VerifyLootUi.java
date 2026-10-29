package workverification;

import batchverification.VerifyMixinTransforms;
import com.llamalad7.mixinextras.MixinExtrasBootstrap;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.util.*;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.transformer.*;

public final class VerifyLootUi {
    static ClassNode node(byte[] bytes){var n=new ClassNode();new ClassReader(bytes).accept(n,0);return n;}
    static byte[] read(ClassLoader l,String name)throws Exception {try(var in=l.getResourceAsStream(name.replace('.','/')+".class")){return in.readAllBytes();}}
    static boolean calls(MethodNode m,String owner,String name) {for(var i:m.instructions)if(i instanceof MethodInsnNode c&&c.owner.equals(owner)&&c.name.equals(name))return true;return false;}
    public static void main(String[] args)throws Exception {
        Path cfg=Path.of(args[0]);Files.createDirectories(cfg);
        Files.writeString(cfg.resolve("offline-new-work.mixins.json"),"{\"required\":true,\"package\":\"local.prominence.work.mixin\",\"compatibilityLevel\":\"JAVA_17\",\"client\":[\"LootTitleSharedMixin\",\"ExplorerDimensionSearchMixin\",\"NatureDimensionSearchMixin\",\"LootRollStaticMixin\",\"LootRollConfigMixin\",\"LootStableKeyMixin\",\"LootChestChanceMixin\",\"LootConditionsVisibleMixin\",\"LootGroupConditionsMixin\",\"LootMobEntityFilterMixin\"],\"injectors\":{\"defaultRequire\":1}}");
        Files.writeString(cfg.resolve("offline-old-smithing.mixins.json"),"{\"required\":true,\"package\":\"local.prominence.compassaliases.mixin\",\"compatibilityLevel\":\"JAVA_17\",\"client\":[\"CompassSearchAliasesMixin\"],\"injectors\":{\"defaultRequire\":1}}");
        Files.writeString(cfg.resolve("offline-old-loot.mixins.json"),"{\"required\":true,\"package\":\"local.prominence.lootdisplay.mixin\",\"compatibilityLevel\":\"JAVA_17\",\"client\":[\"MobLootDisplayMixin\"],\"injectors\":{\"defaultRequire\":1}}");
        var urls=new ArrayList<URL>();urls.add(cfg.toUri().toURL());for(String p:System.getProperty("java.class.path").split(File.pathSeparator))urls.add(Path.of(p).toUri().toURL());
        var resources=new URLClassLoader(urls.toArray(URL[]::new),VerifyLootUi.class.getClassLoader());
        var field=VerifyMixinTransforms.class.getDeclaredField("LOADER");field.setAccessible(true);field.set(null,resources);
        MixinBootstrap.init();var phase=MixinEnvironment.class.getDeclaredMethod("gotoPhase",MixinEnvironment.Phase.class);phase.setAccessible(true);phase.invoke(null,MixinEnvironment.Phase.INIT);MixinExtrasBootstrap.init();
        MixinEnvironment.getCurrentEnvironment().setObfuscationContext("named:intermediary");
        Mixins.addConfiguration("offline-new-work.mixins.json");Mixins.addConfiguration("offline-old-smithing.mixins.json");Mixins.addConfiguration("offline-old-loot.mixins.json");
        for(Config c:Mixins.getConfigs()){c.getConfig().decorate(FabricUtil.KEY_MOD_ID,"prominence_korean_compat");c.getConfig().decorate(FabricUtil.KEY_COMPATIBILITY,FabricUtil.COMPATIBILITY_LATEST);}
        phase.invoke(null,MixinEnvironment.Phase.DEFAULT);MixinEnvironment.getCurrentEnvironment().setObfuscationContext("named:intermediary");
        field=VerifyMixinTransforms.class.getDeclaredField("TRANSFORMER");field.setAccessible(true);var transformer=(IMixinTransformer)field.get(null);
        var targets=List.of("fzzyhmstrs.emi_loot.util.stack.EmiStackWithTitle","com.chaosthedude.explorerscompass.gui.ExplorersCompassScreen","com.chaosthedude.naturescompass.gui.NaturesCompassScreen","fzzyhmstrs.emi_loot.emi.ChestLootRecipe","fzzyhmstrs.emi_loot.emi.ArchaeologyLootRecipe","fzzyhmstrs.emi_loot.emi.MobLootRecipe","fzzyhmstrs.emi_loot.emi.BlockLootRecipe","fzzyhmstrs.emi_loot.emi.GameplayLootRecipe","fzzyhmstrs.emi_loot.util.IconGroupEmiWidget");
        for(String target:targets){var after=node(transformer.transformClassBytes(target,target,read(resources,target)));int hooks=0;boolean aliases=false;
         for(var m:after.methods){new Analyzer<BasicValue>(new BasicVerifier()).analyze(after.name,m);for(var i:m.instructions)if(i instanceof MethodInsnNode x){if(x.owner.equals("local/prominence/work/LootUiText")||x.owner.equals("local/prominence/work/LootNumbers")||x.owner.equals("local/prominence/work/LootEntityDisplay"))hooks++;if(x.owner.equals("local/prominence/compassaliases/StructureSearchNames"))aliases=true;}}
         if(target.endsWith(".MobLootRecipe")){
             var plain=after.methods.stream().filter(m->m.name.contains("plainConditions")).findFirst().orElseThrow();
             var fixture=new ClassNode();fixture.version=org.objectweb.asm.Opcodes.V17;fixture.access=org.objectweb.asm.Opcodes.ACC_PUBLIC;fixture.name="workverification/NativePlainCondition";fixture.superName="java/lang/Object";
             var ctor=new MethodNode(org.objectweb.asm.Opcodes.ACC_PUBLIC,"<init>","()V",null,null);ctor.instructions.add(new VarInsnNode(org.objectweb.asm.Opcodes.ALOAD,0));ctor.instructions.add(new MethodInsnNode(org.objectweb.asm.Opcodes.INVOKESPECIAL,"java/lang/Object","<init>","()V",false));ctor.instructions.add(new InsnNode(org.objectweb.asm.Opcodes.RETURN));ctor.maxStack=1;ctor.maxLocals=1;fixture.methods.add(ctor);
             var copy=new MethodNode(org.objectweb.asm.Opcodes.ACC_PUBLIC,"plain",plain.desc,null,null);plain.accept(copy);copy.name="plain";copy.access=org.objectweb.asm.Opcodes.ACC_PUBLIC;fixture.methods.add(copy);
             var writer=new org.objectweb.asm.ClassWriter(0);fixture.accept(writer);
             var loader=new ClassLoader(VerifyLootUi.class.getClassLoader()){Class<?> define(byte[] bytes){return defineClass("workverification.NativePlainCondition",bytes,0,bytes.length);}};
             var type=loader.define(writer.toByteArray());var receiver=type.getConstructor().newInstance();var message=net.minecraft.class_2561.method_43470("base 5%, looting +3%p");
             var result=(net.minecraft.class_2561)type.getMethod("plain",int.class,net.minecraft.class_2561.class,com.llamalad7.mixinextras.injector.wrapoperation.Operation.class).invoke(receiver,0,message,null);
             if(!result.getString().equals(message.getString()))throw new AssertionError("Native transformed plain condition was changed");
             System.out.println("PASS executed transformed condition tooltip without loading optional symbol rendering");
         }
         if(hooks<1)throw new AssertionError("missing UI hooks "+target);
         if(target.contains("ExplorersCompassScreen")&&!aliases)throw new AssertionError("existing English alias support was lost");
         System.out.println("PASS native EMI/compass transform "+target+" hooks="+hooks);
        }
    }
}
