package inlineverification;

import batchverification.VerifyMixinTransforms;
import com.llamalad7.mixinextras.MixinExtrasBootstrap;
import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.transformer.*;

/** Real installed class bytes, native Mixin transformer; no game, world or graphics. */
public final class VerifyInlineTransforms {
    static List<String> calls(MethodNode m){var r=new ArrayList<String>();for(var i:m.instructions)if(i instanceof MethodInsnNode c)r.add(c.owner+"."+c.name+c.desc);return r;}
    public static void main(String[] args)throws Exception{
        Path root=Path.of(args[0]),work=Path.of(args[1]);Files.createDirectories(work);Path configs=work.resolve("configs");Files.createDirectories(configs);
        String plugin=VerifyMixinTransforms.OfflinePlugin.class.getName();
        for(String file:List.of("prominence-inline-reforge.mixins.json","prominence-inline-reforge.client.mixins.json")){
            Files.writeString(configs.resolve("offline-"+file),Files.readString(root.resolve(file)).replace("local.prominence.backpackinline.InlineGuard",plugin));
        }
        Files.writeString(configs.resolve("offline-legacy-tab.mixins.json"),"""
          {"required":true,"package":"local.prominence.backpackcompat.mixin","compatibilityLevel":"JAVA_17","client":["BackpackReforgeTabMixin","BackpackAnvilCostMixin"],"injectors":{"defaultRequire":1}}
          """);
        Files.writeString(configs.resolve("offline-transfer-item.mixins.json"),"""
          {"required":true,"package":"net.fabricmc.fabric.mixin.transfer","compatibilityLevel":"JAVA_17","mixins":["ItemMixin"],"refmap":"fabric-transfer-api-v1-refmap.json"}
          """);
        var urls=new ArrayList<URL>();urls.add(configs.toUri().toURL());for(String p:System.getProperty("java.class.path").split(File.pathSeparator))if(!p.isBlank())urls.add(Path.of(p).toUri().toURL());
        var resources=new URLClassLoader(urls.toArray(URL[]::new),VerifyInlineTransforms.class.getClassLoader());
        var f=VerifyMixinTransforms.class.getDeclaredField("LOADER");f.setAccessible(true);f.set(null,resources);
        MixinBootstrap.init();Method phase=MixinEnvironment.class.getDeclaredMethod("gotoPhase",MixinEnvironment.Phase.class);phase.setAccessible(true);phase.invoke(null,MixinEnvironment.Phase.INIT);MixinExtrasBootstrap.init();
        MixinEnvironment.getCurrentEnvironment().setSide(MixinEnvironment.Side.CLIENT);MixinEnvironment.getCurrentEnvironment().setObfuscationContext("named:intermediary");
        Mixins.addConfiguration("offline-legacy-tab.mixins.json");Mixins.addConfiguration("offline-prominence-inline-reforge.mixins.json");Mixins.addConfiguration("offline-prominence-inline-reforge.client.mixins.json");
        Mixins.addConfiguration("offline-transfer-item.mixins.json");
        for(Config c:Mixins.getConfigs()){c.getConfig().decorate(FabricUtil.KEY_MOD_ID,"prominence_korean_compat");c.getConfig().decorate(FabricUtil.KEY_COMPATIBILITY,FabricUtil.COMPATIBILITY_LATEST);}
        phase.invoke(null,MixinEnvironment.Phase.DEFAULT);MixinEnvironment.getCurrentEnvironment().setObfuscationContext("named:intermediary");
        f=VerifyMixinTransforms.class.getDeclaredField("TRANSFORMER");f.setAccessible(true);var transformer=(IMixinTransformer)f.get(null);
        int methods=0,bridges=0,preserved=0;var targets=List.of("net.minecraft.class_1735","net.minecraft.class_1703","net.minecraft.class_3244","elocindev.tierify.screen.ReforgeScreenHandler","net.p3pp3rf1y.sophisticatedbackpacks.upgrades.anvil.AnvilUpgradeContainer","net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase","net.p3pp3rf1y.sophisticatedbackpacks.upgrades.anvil.AnvilUpgradeTab");
        for(String target:targets){
            byte[] raw;try(var in=resources.getResourceAsStream(target.replace('.','/')+".class")){raw=in.readAllBytes();}
            byte[] fixed=transformer.transformClassBytes(target,target,raw);if(Arrays.equals(raw,fixed))throw new AssertionError("Untransformed "+target);
            ClassNode before=new ClassNode(),node=new ClassNode();new ClassReader(raw).accept(before,0);new ClassReader(fixed).accept(node,0);
            for(var m:node.methods){new Analyzer<BasicValue>(new BasicVerifier()).analyze(node.name,m);methods++;for(var c:calls(m))if(c.startsWith("local/prominence/backpackinline/"))bridges++;}
            if(target.endsWith("ReforgeScreenHandler")){
                for(String name:List.of("reforge","updateResult","decrementStack","isValidAddition","method_7595")){
                    var old=before.methods.stream().filter(m->m.name.equals(name)).findFirst().orElseThrow();var fresh=node.methods.stream().filter(m->m.name.equals(name)&&m.desc.equals(old.desc)).findFirst().orElseThrow();
                    if(!calls(old).equals(calls(fresh)))throw new AssertionError("Native Tierify mechanics changed: "+name);preserved++;
                }
                if((node.fields.stream().filter(v->v.name.equals("inventory")).findFirst().orElseThrow().access&Opcodes.ACC_FINAL)!=0)throw new AssertionError("Inventory final remains");
            }
            if(target.endsWith("class_1735")&&(node.fields.stream().filter(v->v.name.equals("field_7871")).findFirst().orElseThrow().access&Opcodes.ACC_FINAL)!=0)throw new AssertionError("Slot inventory final remains");
            if(target.endsWith("AnvilUpgradeTab")){
                if(node.methods.stream().noneMatch(m->m.name.contains("prominence$controls")))throw new AssertionError("Inline UI absent");
                if(node.methods.stream().noneMatch(m->calls(m).stream().anyMatch(c->c.contains("PortableReforgeButton.<init>"))))throw new AssertionError("Legacy tab compatibility absent");
            }
            Files.write(work.resolve(target+".class"),fixed);
        }
        if(bridges<12||preserved!=5)throw new AssertionError("Missing bridges: "+bridges);
        // Native Fabric's ItemVariant cache is needed by the separate data-only handler fixture.
        String item="net.minecraft.class_1792";byte[] itemRaw;try(var in=resources.getResourceAsStream(item.replace('.','/')+".class")){itemRaw=in.readAllBytes();}
        byte[] itemFixed=transformer.transformClassBytes(item,item,itemRaw);if(Arrays.equals(itemRaw,itemFixed))throw new AssertionError("Native transfer ItemMixin not applied");
        Path itemFile=work.resolve("data-classes/net/minecraft/class_1792.class");Files.createDirectories(itemFile.getParent());Files.write(itemFile,itemFixed);
        Files.writeString(work.resolve("native-mixin-report.json"),"{\"status\":\"passed\",\"targets\":7,\"methodsBytecodeVerified\":"+methods+",\"nativeBodiesPreserved\":5,\"bridges\":"+bridges+",\"gameStarted\":false}");
        System.out.println("PASS inline native Mixin: 7 targets, "+methods+" methods, "+bridges+" bridges; original Tierify mechanics preserved");
    }
}
