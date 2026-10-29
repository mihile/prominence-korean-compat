package inlineverification;
import batchverification.VerifyMixinTransforms;
import com.llamalad7.mixinextras.MixinExtrasBootstrap;
import java.io.File;
import java.lang.reflect.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.transformer.*;

/** Real native language factory and lookup, with an old-Paxi-like in-memory resource. */
public final class VerifyTranslationTransforms {
    public static void main(String[]args)throws Exception{
        Path root=Path.of(args[0]),work=Path.of(args[1]);Files.createDirectories(work);Path configs=work.resolve("configs");Files.createDirectories(configs);
        Files.writeString(configs.resolve("offline-translation.mixins.json"),Files.readString(root.resolve("prominence-translation.client.mixins.json")).replace("local.prominence.localization.TranslationGuard",VerifyMixinTransforms.OfflinePlugin.class.getName()));
        var urls=new ArrayList<URL>();urls.add(configs.toUri().toURL());for(String p:System.getProperty("java.class.path").split(File.pathSeparator))if(!p.isBlank())urls.add(Path.of(p).toUri().toURL());
        var resources=new URLClassLoader(urls.toArray(URL[]::new),VerifyTranslationTransforms.class.getClassLoader());var f=VerifyMixinTransforms.class.getDeclaredField("LOADER");f.setAccessible(true);f.set(null,resources);
        MixinBootstrap.init();Method phase=MixinEnvironment.class.getDeclaredMethod("gotoPhase",MixinEnvironment.Phase.class);phase.setAccessible(true);phase.invoke(null,MixinEnvironment.Phase.INIT);MixinExtrasBootstrap.init();MixinEnvironment.getCurrentEnvironment().setSide(MixinEnvironment.Side.CLIENT);MixinEnvironment.getCurrentEnvironment().setObfuscationContext("named:intermediary");
        Mixins.addConfiguration("offline-translation.mixins.json");
        for(Config c:Mixins.getConfigs()){c.getConfig().decorate(FabricUtil.KEY_MOD_ID,"prominence_korean_compat");c.getConfig().decorate(FabricUtil.KEY_COMPATIBILITY,FabricUtil.COMPATIBILITY_LATEST);}
        phase.invoke(null,MixinEnvironment.Phase.DEFAULT);MixinEnvironment.getCurrentEnvironment().setObfuscationContext("named:intermediary");f=VerifyMixinTransforms.class.getDeclaredField("TRANSFORMER");f.setAccessible(true);var transformer=(IMixinTransformer)f.get(null);
        String target="net.minecraft.class_1078";byte[] raw;try(var in=resources.getResourceAsStream(target.replace('.','/')+".class")){raw=in.readAllBytes();}
        byte[] fixed=transformer.transformClassBytes(target,target,raw);if(Arrays.equals(raw,fixed))throw new AssertionError("No language transform");
        var before=new ClassNode();var after=new ClassNode();new ClassReader(raw).accept(before,0);new ClassReader(fixed).accept(after,0);
        for(var method:after.methods)new Analyzer<BasicValue>(new BasicVerifier()).analyze(after.name,method);
        for(var method:before.methods)if(!method.name.equals("method_4675")){
            var unchanged=after.methods.stream().filter(m->m.name.equals(method.name)&&m.desc.equals(method.desc)).findFirst().orElseThrow();
            if(!calls(method).equals(calls(unchanged)))throw new AssertionError("Unrelated native lookup/format method changed");
        }
        var factory=after.methods.stream().filter(m->m.name.equals("method_4675")).findFirst().orElseThrow();
        if(calls(factory).stream().filter(c->c.contains("reviewedLanguage")).count()!=1)throw new AssertionError("Exactly one final map overlay expected");
        Path fixture=work.resolve("classes/net/minecraft/class_1078.class");Files.createDirectories(fixture.getParent());Files.write(fixture,fixed);
        try(var loader=new URLClassLoader(new URL[]{work.resolve("classes").toUri().toURL()},VerifyTranslationTransforms.class.getClassLoader()){
            protected Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException{
                if(name.equals("net.minecraft.class_1078")){synchronized(getClassLoadingLock(name)){var c=findLoadedClass(name);if(c==null)c=findClass(name);if(resolve)resolveClass(c);return c;}}
                return super.loadClass(name,resolve);
            }
        }){
            var nativeLanguage=loader.loadClass(target);var manager=Class.forName("net.minecraft.class_3300",false,loader);
            var packType=Class.forName("net.minecraft.class_3262",false,loader);var supplierType=Class.forName("net.minecraft.class_7367",false,loader);
            var oldPack=java.lang.reflect.Proxy.newProxyInstance(loader,new Class<?>[]{packType},(proxy,method,values)->{
                if(method.getName().equals("method_14409"))return "Old Paxi fixture";
                throw new AssertionError("Unexpected pack call "+method);
            });
            var stream=java.lang.reflect.Proxy.newProxyInstance(loader,new Class<?>[]{supplierType},(proxy,method,values)->{
                if(method.getName().equals("get"))return new java.io.ByteArrayInputStream("{\"item.croptopia.calamari\":\"오징어\",\"unrelated.key\":\"팩의 기존 문구\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
                throw new AssertionError("Unexpected supplier call "+method);
            });
            var resource=Class.forName("net.minecraft.class_3298",false,loader).getConstructor(packType,supplierType).newInstance(oldPack,stream);
            Object empty=java.lang.reflect.Proxy.newProxyInstance(loader,new Class<?>[]{manager},(proxy,method,values)->{
                if(method.getName().equals("method_14487"))return Set.of("fixture");
                if(method.getName().equals("method_14489"))return values[0].toString().endsWith("lang/ko_kr.json")?List.of(resource):List.of();
                if(method.getName().equals("toString"))return "Empty CPU language resource fixture";
                throw new AssertionError("Unexpected manager call "+method);
            });
            var load=nativeLanguage.getMethod("method_4675",manager,List.class,boolean.class);var get=nativeLanguage.getMethod("method_4679",String.class,String.class);var has=nativeLanguage.getMethod("method_4678",String.class);
            var ko=load.invoke(null,empty,List.of("en_us","ko_kr"),false);
            if(!get.invoke(ko,"item.croptopia.calamari","fallback").equals("오징어 다리"))throw new AssertionError("Native Korean factory did not apply JAR corrections");
            if(!get.invoke(ko,"structure.bosses_of_mass_destruction.obsidilith_arena","fallback").equals("옵시딜리스 제단"))throw new AssertionError("Structure correction missing");
            if(!get.invoke(ko,"attribute.name.generic.luck","행운").equals("운"))throw new AssertionError("Native Luck attribute correction missing");
            if(!get.invoke(ko,"attribute.puffish_attributes.fortune","행운").equals("채굴 행운"))throw new AssertionError("Native mining Fortune correction missing");
            if(!has.invoke(ko,"dialogue.skellak.molten_core.1_2").equals(true))throw new AssertionError("Native containsKey not updated");
            if(!get.invoke(ko,"unrelated.key","original fallback").equals("팩의 기존 문구"))throw new AssertionError("Unrelated old Paxi translation changed");
            if(!get.invoke(ko,"missing.key","original fallback").equals("original fallback"))throw new AssertionError("Native fallback changed");
            var en=load.invoke(null,empty,List.of("en_us"),true);
            if(!get.invoke(en,"item.croptopia.calamari","English fallback").equals("English fallback"))throw new AssertionError("Korean leaked into English");
            if(!nativeLanguage.getMethod("method_29428").invoke(en).equals(true))throw new AssertionError("Native RTL flag changed");
            var koAgain=load.invoke(null,empty,List.of("en_us","ko_kr"),false);
            if(!get.invoke(koAgain,"item.croptopia.calamari","fallback").equals("오징어 다리"))throw new AssertionError("Resource reload failed");
        }
        Files.writeString(work.resolve("report.json"),"{\"status\":\"passed\",\"nativeTargets\":1,\"nativeMethodsVerified\":"+after.methods.size()+",\"nativeLanguageRuntimeChecks\":10,\"oldPaxiLikeResourceLoaded\":true,\"gameStarted\":false}");
        System.out.println("PASS native language mixin, "+after.methods.size()+" bytecode methods and 10 actual language factory/old pack priority/lookup/reload checks; no Minecraft client, world or graphics launched");
    }
    private static List<String> calls(MethodNode method){var out=new ArrayList<String>();for(var i:method.instructions)if(i instanceof MethodInsnNode c)out.add(c.owner+"."+c.name+c.desc);return out;}
}
