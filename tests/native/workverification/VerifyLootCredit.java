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

public final class VerifyLootCredit {
    static ClassNode node(byte[] bytes){var n=new ClassNode();new ClassReader(bytes).accept(n,0);return n;}
    static byte[] read(ClassLoader l,String name)throws Exception {try(var in=l.getResourceAsStream(name.replace('.','/')+".class")){return in.readAllBytes();}}
    static boolean calls(MethodNode m,String owner,String name) {for(var i:m.instructions)if(i instanceof MethodInsnNode c&&c.owner.equals(owner)&&c.name.equals(name))return true;return false;}
    public static void main(String[] args)throws Exception {
        Path cfg=Path.of(args[0]);Files.createDirectories(cfg);
        var config=com.google.gson.JsonParser.parseString(Files.readString(Path.of(args[1]))).getAsJsonObject();config.remove("plugin");
        Files.writeString(cfg.resolve("offline-loot.mixins.json"),config.toString());
        var urls=new ArrayList<URL>();urls.add(cfg.toUri().toURL());for(String p:System.getProperty("java.class.path").split(File.pathSeparator))urls.add(Path.of(p).toUri().toURL());
        var resources=new URLClassLoader(urls.toArray(URL[]::new),VerifyLootCredit.class.getClassLoader());
        var field=VerifyMixinTransforms.class.getDeclaredField("LOADER");field.setAccessible(true);field.set(null,resources);
        MixinBootstrap.init();var phase=MixinEnvironment.class.getDeclaredMethod("gotoPhase",MixinEnvironment.Phase.class);phase.setAccessible(true);phase.invoke(null,MixinEnvironment.Phase.INIT);MixinExtrasBootstrap.init();
        MixinEnvironment.getCurrentEnvironment().setObfuscationContext("named:intermediary");
        Mixins.addConfiguration("offline-loot.mixins.json");
        for(Config c:Mixins.getConfigs()){c.getConfig().decorate(FabricUtil.KEY_MOD_ID,"prominence_korean_compat");c.getConfig().decorate(FabricUtil.KEY_COMPATIBILITY,FabricUtil.COMPATIBILITY_LATEST);}
        phase.invoke(null,MixinEnvironment.Phase.DEFAULT);MixinEnvironment.getCurrentEnvironment().setObfuscationContext("named:intermediary");
        field=VerifyMixinTransforms.class.getDeclaredField("TRANSFORMER");field.setAccessible(true);var transformer=(IMixinTransformer)field.get(null);
        var targets=List.of("net.minecraft.class_1309","net.minecraft.class_1282","net.minecraft.class_1676","net.minecraft.class_1293","net.minecraft.class_1890","net.minecraft.class_1799","chronosacaria.mcdw.effects.EnchantmentEffects","dev.shadowsoffire.apotheosis.ench.enchantments.masterwork.ScavengerEnchant","net.soulsweaponry.entity.projectile.LeviathanAxeEntity","net.sweenus.simplyswords.power.powers.ImbuedPower","net.sweenus.simplyswords.power.powers.PincushionPower","net.sweenus.simplyswords.world.SoulkeeperLanternManager","net.sweenus.simplyswords.util.HelperMethods","net.sweenus.simplyswords.power.powers.EchoPower","net.sweenus.simplyswords.api.SimplySwordsAPI","net.soulsweaponry.entitydata.BleedData","net.soulsweaponry.api.entitystats.EntityBleed");
        for(String target:targets){var after=node(transformer.transformClassBytes(target,target,read(resources,target)));int hooks=0;
         for(var m:after.methods){new Analyzer<BasicValue>(new BasicVerifier()).analyze(after.name,m);for(var i:m.instructions)if(i instanceof MethodInsnNode x&&x.owner.startsWith("local/prominence/lootcredit/"))hooks++;}
         if(hooks<1&&!after.interfaces.contains("local/prominence/lootcredit/CreditHolder"))throw new AssertionError("missing loot hooks "+target);
         if(target.equals("net.minecraft.class_1309")){for(var m:after.methods)if(m.name.contains("credit$damage")&&calls(m,"local/prominence/lootcredit/LootCredit","attributed"))throw new AssertionError("combat source must stay untouched; only enrich loot source");}
         System.out.println("PASS native loot-credit transform "+target+" hooks="+hooks);
        }
    }
}
