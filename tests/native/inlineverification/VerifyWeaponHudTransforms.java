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
public final class VerifyWeaponHudTransforms {
    static List<String> calls(MethodNode m){var out=new ArrayList<String>();for(var i:m.instructions)if(i instanceof MethodInsnNode c)out.add(c.owner+"."+c.name+c.desc);return out;}
    public static void main(String[]args)throws Exception{
        Path root=Path.of(args[0]),work=Path.of(args[1]);Files.createDirectories(work);Path configs=work.resolve("configs");Files.createDirectories(configs);
        Files.writeString(configs.resolve("offline-weapons.mixins.json"),Files.readString(root.resolve("prominence-weapon-hud.client.mixins.json")).replace("local.prominence.weaponhud.WeaponHudGuard",VerifyMixinTransforms.OfflinePlugin.class.getName()));
        Files.writeString(configs.resolve("offline-engine-interaction.mixins.json"),"""
        {"required":true,"package":"net.spell_engine.mixin.client.control","compatibilityLevel":"JAVA_17","client":["ClientPlayerInteractionManagerMixin"],"refmap":"spell_engine-refmap.json","injectors":{"defaultRequire":1}}
        """);
        Files.writeString(configs.resolve("offline-existing-hud.mixins.json"),"""
        {"required":true,"package":"local.prominence.trinketalloc.mixin","compatibilityLevel":"JAVA_17","client":["SpellHudScopeMixin"],"injectors":{"defaultRequire":1}}
        """);
        var urls=new ArrayList<URL>();urls.add(configs.toUri().toURL());for(String p:System.getProperty("java.class.path").split(File.pathSeparator))if(!p.isBlank())urls.add(Path.of(p).toUri().toURL());
        var resources=new URLClassLoader(urls.toArray(URL[]::new),VerifyWeaponHudTransforms.class.getClassLoader());var f=VerifyMixinTransforms.class.getDeclaredField("LOADER");f.setAccessible(true);f.set(null,resources);
        MixinBootstrap.init();Method phase=MixinEnvironment.class.getDeclaredMethod("gotoPhase",MixinEnvironment.Phase.class);phase.setAccessible(true);phase.invoke(null,MixinEnvironment.Phase.INIT);MixinExtrasBootstrap.init();MixinEnvironment.getCurrentEnvironment().setSide(MixinEnvironment.Side.CLIENT);MixinEnvironment.getCurrentEnvironment().setObfuscationContext("named:intermediary");
        for(String name:List.of("offline-engine-interaction.mixins.json","offline-existing-hud.mixins.json","offline-weapons.mixins.json"))Mixins.addConfiguration(name);
        for(Config c:Mixins.getConfigs()){c.getConfig().decorate(FabricUtil.KEY_MOD_ID,"prominence_korean_compat");c.getConfig().decorate(FabricUtil.KEY_COMPATIBILITY,FabricUtil.COMPATIBILITY_LATEST);}
        phase.invoke(null,MixinEnvironment.Phase.DEFAULT);MixinEnvironment.getCurrentEnvironment().setObfuscationContext("named:intermediary");f=VerifyMixinTransforms.class.getDeclaredField("TRANSFORMER");f.setAccessible(true);var transformer=(IMixinTransformer)f.get(null);
        int verified=0;Path fixture=work.resolve("data-classes");
        for(String target:List.of("net.minecraft.class_636","net.minecraft.class_1796$class_1797","net.minecraft.class_1796","net.sweenus.simplyswords.client.AbilityKeybindHandler","net.sweenus.simplyswords.entity.ThrownSwordEntity","net.spell_engine.client.gui.HudRenderHelper")){
            byte[] raw;try(var in=resources.getResourceAsStream(target.replace('.','/')+".class")){raw=in.readAllBytes();}byte[] fixed=transformer.transformClassBytes(target,target,raw);if(Arrays.equals(raw,fixed))throw new AssertionError("Untransformed "+target);
            var before=new ClassNode();var after=new ClassNode();new ClassReader(raw).accept(before,0);new ClassReader(fixed).accept(after,0);
            for(var m:after.methods){new Analyzer<BasicValue>(new BasicVerifier()).analyze(after.name,m);verified++;}
            if(target.equals("net.minecraft.class_636")){
                var use=after.methods.stream().filter(m->m.name.equals("method_2919")).findFirst().orElseThrow();
                var inputCalls=calls(use).stream().filter(c->!c.startsWith("org/spongepowered/")).toList();
                if(!inputCalls.get(0).contains("prominence$weaponUse"))throw new AssertionError("Guard must precede item-use packet path: "+inputCalls.subList(0,Math.min(7,inputCalls.size())));
                long results=after.methods.stream().filter(m->List.of("method_2896","method_2905","method_2917").contains(m.name)).filter(m->calls(m).stream().anyMatch(c->c.contains("prominence$interactionResult"))).count();if(results!=3)throw new AssertionError("Block/entity outcomes not all covered");
            }
            if(target.equals("net.minecraft.class_1796"))for(var m:before.methods){var same=after.methods.stream().filter(x->x.name.equals(m.name)&&x.desc.equals(m.desc)).findFirst().orElseThrow();if(!calls(m).equals(calls(same)))throw new AssertionError("Native cooldown mutations changed");}
            if(target.equals("net.spell_engine.client.gui.HudRenderHelper")&&after.methods.stream().noneMatch(m->m.name.contains("prominence$nativeWeaponCard")))throw new AssertionError("HUD hook absent");
            Path file=fixture.resolve(target.replace('.','/')+".class");Files.createDirectories(file.getParent());Files.write(file,fixed);
        }
        // Run the transformed native cooldown manager/entry without constructing any item or world.
        try(var loader=new URLClassLoader(new URL[]{fixture.toUri().toURL()},VerifyWeaponHudTransforms.class.getClassLoader()){
            protected Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException{
                if(name.equals("net.minecraft.class_1796")||name.equals("net.minecraft.class_1796$class_1797")){synchronized(getClassLoadingLock(name)){var c=findLoadedClass(name);if(c==null)c=findClass(name);if(resolve)resolveClass(c);return c;}}
                return super.loadClass(name,resolve);
            }
        }){
            var type=loader.loadClass("net.minecraft.class_1796");var manager=type.getConstructor().newInstance();var item=Class.forName("net.minecraft.class_1792",false,loader);var set=type.getMethod("method_7906",item,int.class);var tick=type.getMethod("method_7903");var remaining=type.getMethod("prominence$remaining",item);var progress=type.getMethod("method_7905",item,float.class);
            set.invoke(manager,null,80);if(!remaining.invoke(manager,new Object[]{null}).equals(80))throw new AssertionError("Initial native cooldown");for(int i=0;i<20;i++)tick.invoke(manager);if(!remaining.invoke(manager,new Object[]{null}).equals(60))throw new AssertionError("Native tick countdown");if(Math.abs((Float)progress.invoke(manager,null,0f)-.75f)>.0001)throw new AssertionError("Native shaded progress");set.invoke(manager,null,5);for(int i=0;i<5;i++)tick.invoke(manager);if(!remaining.invoke(manager,new Object[]{null}).equals(0))throw new AssertionError("Expiry");
        }
        Files.writeString(work.resolve("native-mixin-report.json"),"{\"status\":\"passed\",\"targets\":6,\"methodsBytecodeVerified\":"+verified+",\"nativeCooldownRuntimeChecks\":4,\"nativeCooldownMutationsPreserved\":true,\"gameStarted\":false}");
        System.out.println("PASS native weapon HUD/input transforms: 6 targets, "+verified+" methods; original spell HUD scope and engine input mixins coexist; native cooldown countdown/progress/expiry verified without items or world");
    }
}
