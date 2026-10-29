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

public final class VerifyEmiRefresh {
    static ClassNode node(byte[] bytes){var n=new ClassNode();new ClassReader(bytes).accept(n,0);return n;}
    static byte[] read(ClassLoader l,String name)throws Exception {try(var in=l.getResourceAsStream(name.replace('.','/')+".class")){return in.readAllBytes();}}
    static boolean calls(MethodNode m,String owner,String name) {for(var i:m.instructions)if(i instanceof MethodInsnNode c&&c.owner.equals(owner)&&c.name.equals(name))return true;return false;}
    public static void main(String[] args)throws Exception {
        Path cfg=Path.of(args[0]);Files.createDirectories(cfg);
        Files.writeString(cfg.resolve("offline-new-work.mixins.json"),"{\"required\":true,\"package\":\"local.prominence.work.mixin\",\"compatibilityLevel\":\"JAVA_17\",\"client\":[\"EmiCraftableRefreshMixin\"],\"injectors\":{\"defaultRequire\":1}}");
        var urls=new ArrayList<URL>();urls.add(cfg.toUri().toURL());for(String p:System.getProperty("java.class.path").split(File.pathSeparator))urls.add(Path.of(p).toUri().toURL());
        var resources=new URLClassLoader(urls.toArray(URL[]::new),VerifyEmiRefresh.class.getClassLoader());
        var field=VerifyMixinTransforms.class.getDeclaredField("LOADER");field.setAccessible(true);field.set(null,resources);
        MixinBootstrap.init();var phase=MixinEnvironment.class.getDeclaredMethod("gotoPhase",MixinEnvironment.Phase.class);phase.setAccessible(true);phase.invoke(null,MixinEnvironment.Phase.INIT);MixinExtrasBootstrap.init();
        MixinEnvironment.getCurrentEnvironment().setObfuscationContext("named:intermediary");
        Mixins.addConfiguration("offline-new-work.mixins.json");
        for(Config c:Mixins.getConfigs()){c.getConfig().decorate(FabricUtil.KEY_MOD_ID,"prominence_korean_compat");c.getConfig().decorate(FabricUtil.KEY_COMPATIBILITY,FabricUtil.COMPATIBILITY_LATEST);}
        phase.invoke(null,MixinEnvironment.Phase.DEFAULT);MixinEnvironment.getCurrentEnvironment().setObfuscationContext("named:intermediary");
        field=VerifyMixinTransforms.class.getDeclaredField("TRANSFORMER");field.setAccessible(true);var transformer=(IMixinTransformer)field.get(null);
        String target="dev.emi.emi.screen.EmiScreenManager";
        var after=node(transformer.transformClassBytes(target,target,read(resources,target)));
        boolean defers=false,refreshes=false,deleteNative=false,inventoryNative=false;
        int methods=0;
        for(var m:after.methods){new Analyzer<BasicValue>(new BasicVerifier()).analyze(after.name,m);methods++;
            for(var i:m.instructions)if(i instanceof MethodInsnNode x){
                if(x.owner.equals("local/prominence/work/CraftableRefresh")&&x.name.equals("shouldCompute"))defers=true;
                if(x.owner.equals("local/prominence/work/CraftableRefresh")&&x.name.equals("needsRefresh"))refreshes=true;
                if(x.owner.equals("dev/emi/emi/network/EmiNetwork")&&x.name.equals("sendToServer"))deleteNative=true;
                if(x.owner.equals("dev/emi/emi/api/recipe/EmiPlayerInventory")&&x.name.equals("of"))inventoryNative=true;
            }
        }
        if(!defers||!refreshes||!deleteNative||!inventoryNative)throw new AssertionError("Incomplete EMI integration or original deletion/inventory path removed");
        System.out.println("PASS actual EMI1.1.24 transform/ASM methods="+methods+"; hidden deferral, visibility invalidation, native inventory and network deletion retained");
    }
}
