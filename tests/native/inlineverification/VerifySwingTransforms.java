package inlineverification;
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

public final class VerifySwingTransforms {
    static long count(ClassNode n,String name){return n.methods.stream().flatMap(m->{var calls=new ArrayList<MethodInsnNode>();for(var i:m.instructions)if(i instanceof MethodInsnNode c)calls.add(c);return calls.stream();}).filter(c->c.name.equals(name)).count();}
    public static void main(String[]args)throws Exception{
        Path root=Path.of(args[0]),work=Path.of(args[1]);Files.createDirectories(work);Path configs=work.resolve("configs");Files.createDirectories(configs);
        Files.writeString(configs.resolve("offline-swing.mixins.json"),Files.readString(root.resolve("prominence-swing-scope.mixins.json")).replace("local.prominence.swingscope.SwingScopeGuard",VerifyMixinTransforms.OfflinePlugin.class.getName()));
        Files.writeString(configs.resolve("offline-native-swing.mixins.json"),"""
        {"required":true,"package":"net.sweenus.simplyswords.mixin","compatibilityLevel":"JAVA_17","mixins":["LivingEntityMixin","compat.BetterCombatServerNetworkMixin"],"injectors":{"defaultRequire":1}}
        """);
        var urls=new ArrayList<URL>();urls.add(configs.toUri().toURL());for(String p:System.getProperty("java.class.path").split(File.pathSeparator))if(!p.isBlank())urls.add(Path.of(p).toUri().toURL());
        var resources=new URLClassLoader(urls.toArray(URL[]::new),VerifySwingTransforms.class.getClassLoader());var f=VerifyMixinTransforms.class.getDeclaredField("LOADER");f.setAccessible(true);f.set(null,resources);
        MixinBootstrap.init();Method phase=MixinEnvironment.class.getDeclaredMethod("gotoPhase",MixinEnvironment.Phase.class);phase.setAccessible(true);phase.invoke(null,MixinEnvironment.Phase.INIT);MixinExtrasBootstrap.init();MixinEnvironment.getCurrentEnvironment().setSide(MixinEnvironment.Side.SERVER);MixinEnvironment.getCurrentEnvironment().setObfuscationContext("named:intermediary");
        for(String name:List.of("offline-native-swing.mixins.json","offline-swing.mixins.json"))Mixins.addConfiguration(name);
        for(Config c:Mixins.getConfigs()){c.getConfig().decorate(FabricUtil.KEY_MOD_ID,"prominence_korean_compat");c.getConfig().decorate(FabricUtil.KEY_COMPATIBILITY,FabricUtil.COMPATIBILITY_LATEST);}
        phase.invoke(null,MixinEnvironment.Phase.DEFAULT);MixinEnvironment.getCurrentEnvironment().setObfuscationContext("named:intermediary");f=VerifyMixinTransforms.class.getDeclaredField("TRANSFORMER");f.setAccessible(true);var transformer=(IMixinTransformer)f.get(null);
        int verified=0;
        for(String target:List.of("net.minecraft.class_1309","net.bettercombat.network.ServerNetwork","net.sweenus.simplyswords.api.SimplySwordsAPI")){
            byte[] raw;try(var in=resources.getResourceAsStream(target.replace('.','/')+".class")){raw=in.readAllBytes();}byte[] fixed=transformer.transformClassBytes(target,target,raw);
            if(Arrays.equals(raw,fixed))throw new AssertionError("Untransformed "+target);
            var after=new ClassNode();new ClassReader(fixed).accept(after,0);
            for(var m:after.methods){new Analyzer<BasicValue>(new BasicVerifier()).analyze(after.name,m);verified++;}
            if(target.equals("net.minecraft.class_1309")){
                if(count(after,"enterAnimation")!=1||count(after,"exitAnimation")<1||count(after,"onWeaponSwing")!=1)throw new AssertionError("Animation scope missing");
                var wrapped=after.methods.stream().filter(m->m.name.contains("animationScope")).findFirst().orElseThrow();
                if(wrapped.tryCatchBlocks.isEmpty())throw new AssertionError("Scope cleanup must include exceptional exits");
            }else if(target.equals("net.bettercombat.network.ServerNetwork")){
                var attack=after.methods.stream().filter(m->m.name.equals("lambda$initializeHandlers$5")).findFirst().orElseThrow();
                boolean nativeHook=false;for(var i:attack.instructions)if(i instanceof MethodInsnNode c&&c.name.contains("simplyswords$triggerRunicSlashFromBetterCombat"))nativeHook=true;
                if(!nativeHook||count(after,"triggerRunicSlash")!=1||count(after,"allowAnimation")!=0)throw new AssertionError("Real attack hook must remain untouched");
            }else{
                if(count(after,"blockAnimationPower")!=1)throw new AssertionError("API scope check missing");
                var api=after.methods.stream().filter(m->m.name.equals("onWeaponSwing")).findFirst().orElseThrow();
                boolean first=false;for(var i:api.instructions)if(i instanceof MethodInsnNode c&&!c.owner.startsWith("org/spongepowered/")){first=c.name.contains("combatOnly");break;}
                if(!first)throw new AssertionError("Guard must precede gem and weapon powers");
            }
            Path file=work.resolve(target.replace('.','/')+".class");Files.createDirectories(file.getParent());Files.write(file,fixed);
        }
        Files.writeString(work.resolve("report.json"),"{\"status\":\"passed\",\"targets\":3,\"methodsBytecodeVerified\":"+verified+",\"fullNativeMixinsApplied\":true,\"gameStarted\":false}");
        System.out.println("PASS full native Simply Swords + Better Combat + compatibility transformations: 3 targets, "+verified+" verified methods; real attack hook preserved");
    }
}
