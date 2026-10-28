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

public final class VerifyBackpackAndRemap {
    static ClassNode node(byte[] bytes){var n=new ClassNode();new ClassReader(bytes).accept(n,0);return n;}
    static byte[] read(ClassLoader l,String name)throws Exception {try(var in=l.getResourceAsStream(name.replace('.','/')+".class")){return in.readAllBytes();}}
    static boolean calls(MethodNode m,String owner,String name) {for(var i:m.instructions)if(i instanceof MethodInsnNode c&&c.owner.equals(owner)&&c.name.equals(name))return true;return false;}
    public static void main(String[] args)throws Exception {
        Path cfg=Path.of(args[0]);Files.createDirectories(cfg);
        Files.writeString(cfg.resolve("offline-new-work.mixins.json"),"{\"required\":true,\"package\":\"local.prominence.work.mixin\",\"compatibilityLevel\":\"JAVA_17\",\"client\":[\"BackpackStonecuttingMixin\",\"UnchangedStateRemapMixin\"],\"injectors\":{\"defaultRequire\":1}}");
        Files.writeString(cfg.resolve("offline-old-smithing.mixins.json"),"{\"required\":true,\"package\":\"local.prominence.backpackcompat.mixin\",\"compatibilityLevel\":\"JAVA_17\",\"client\":[\"BackpackSmithingEmiMixin\"],\"injectors\":{\"defaultRequire\":1}}");
        var urls=new ArrayList<URL>();urls.add(cfg.toUri().toURL());for(String p:System.getProperty("java.class.path").split(File.pathSeparator))urls.add(Path.of(p).toUri().toURL());
        var resources=new URLClassLoader(urls.toArray(URL[]::new),VerifyBackpackAndRemap.class.getClassLoader());
        var field=VerifyMixinTransforms.class.getDeclaredField("LOADER");field.setAccessible(true);field.set(null,resources);
        MixinBootstrap.init();var phase=MixinEnvironment.class.getDeclaredMethod("gotoPhase",MixinEnvironment.Phase.class);phase.setAccessible(true);phase.invoke(null,MixinEnvironment.Phase.INIT);MixinExtrasBootstrap.init();
        MixinEnvironment.getCurrentEnvironment().setObfuscationContext("named:intermediary");
        Mixins.addConfiguration("offline-new-work.mixins.json");Mixins.addConfiguration("offline-old-smithing.mixins.json");
        for(Config c:Mixins.getConfigs()){c.getConfig().decorate(FabricUtil.KEY_MOD_ID,"prominence_korean_compat");c.getConfig().decorate(FabricUtil.KEY_COMPATIBILITY,FabricUtil.COMPATIBILITY_LATEST);}
        phase.invoke(null,MixinEnvironment.Phase.DEFAULT);MixinEnvironment.getCurrentEnvironment().setObfuscationContext("named:intermediary");
        field=VerifyMixinTransforms.class.getDeclaredField("TRANSFORMER");field.setAccessible(true);var transformer=(IMixinTransformer)field.get(null);
        var targets=List.of("net.p3pp3rf1y.sophisticatedcore.compat.emi.EmiGridMenuInfo","net.fabricmc.fabric.impl.registry.sync.trackers.StateIdTracker");
        for(String target:targets) {
            var transformed=node(transformer.transformClassBytes(target,target,read(resources,target)));
            int hooks=0;boolean smithing=false;
            for(var m:transformed.methods) {
                new Analyzer<BasicValue>(new BasicVerifier()).analyze(transformed.name,m);
                for(var i:m.instructions)if(i instanceof MethodInsnNode c) {
                    if(c.owner.equals("local/prominence/work/BackpackStonecutting")||c.owner.equals("local/prominence/work/StateOrder"))hooks++;
                    if(c.owner.equals("local/prominence/backpackcompat/client/BackpackSmithingTransfers")||c.owner.endsWith("/BackpackSmithingTransfers"))smithing=true;
                }
            }
            if(hooks<1)throw new AssertionError("missing new hooks "+target);
            if(target.endsWith("EmiGridMenuInfo")&&(!smithing||hooks!=6))throw new AssertionError("six stone hooks and previous smithing must coexist: "+hooks+" "+smithing);
            System.out.println("PASS: native transformed/ASM verified "+target+" hooks="+hooks);
        }
        String stone="net/p3pp3rf1y/sophisticatedcore/upgrades/stonecutter/StonecutterRecipeContainer";
        var input=node(read(resources,stone+"$1"));
        if(input.methods.stream().noneMatch(m->m.name.equals("method_7668")&&calls(m,stone,"onCraftMatrixChanged")))throw new AssertionError("input does not rebuild recipes synchronously");
        var container=node(read(resources,stone));
        var selection=container.methods.stream().filter(m->m.name.equals("selectRecipe")).findFirst().orElseThrow();
        if(!calls(selection,stone,"updateRecipeResultSlot")||!calls(selection,"net/p3pp3rf1y/sophisticatedcore/common/gui/IServerUpdater","sendDataToServer"))throw new AssertionError("native selection contract changed");
        var filler=node(read(resources,"dev/emi/emi/registry/EmiRecipeFiller"));
        if(filler.methods.stream().noneMatch(m->m.name.equals("clientFill")&&calls(m,"net/minecraft/class_636","method_2906")))throw new AssertionError("native fill is not normal slot clicks");
        var handler=node(read(resources,"local/prominence/work/BackpackStonecutting$1"));
        if(handler.methods.stream().noneMatch(m->m.name.equals("fill")&&calls(m,"dev/emi/emi/registry/EmiRecipeFiller","clientFill")))throw new AssertionError("live fill adapter not wired");
        if(handler.methods.stream().noneMatch(m->m.name.equals("select")&&calls(m,stone,"selectRecipe")))throw new AssertionError("live selection adapter not wired");
        if(handler.methods.stream().noneMatch(m->m.name.equals("take")&&calls(m,"net/minecraft/class_636","method_2906")))throw new AssertionError("live take adapter not wired");
        System.out.println("PASS: installed native input refresh, local/server selection and live click-fill adapter; no new server channel");
    }
}
