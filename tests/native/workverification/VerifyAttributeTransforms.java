package workverification;

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

public final class VerifyAttributeTransforms {
    public static void record(Object stack,Object property,Object attributes,Object modifiers)throws Exception{
        @SuppressWarnings("unchecked") var map=(com.google.common.collect.Multimap<Object,Object>)modifiers;
        map.put("applied",property.getClass().getField("item").get(property)+":"+attributes);
    }
    static final class Loader extends ClassLoader {
        final Map<String,byte[]> definitions;
        Loader(Map<String,byte[]> d){super(VerifyAttributeTransforms.class.getClassLoader());definitions=d;}
        protected synchronized Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException{
            Class<?> type=findLoadedClass(name);if(type==null&&definitions.containsKey(name)){byte[] b=definitions.get(name);type=defineClass(name,b,0,b.length);}if(type==null)type=super.loadClass(name,false);if(resolve)resolveClass(type);return type;
        }
    }
    static String calls(MethodNode method){var result=new StringBuilder();for(var i:method.instructions)if(i instanceof MethodInsnNode m)result.append(m.getOpcode()).append(m.owner).append(m.name).append(m.desc);return result.toString();}
    public static void main(String[]args)throws Exception{
        Path output=Path.of(args[0]);boolean optimized=Boolean.parseBoolean(args[1]);Path fixture=Path.of(args[2]);Files.createDirectories(output);Path cfg=output.resolve("config");Files.createDirectories(cfg);
        var configs=new ArrayList<String>();
        for(String name:List.of("prominence-emi-item-lookups.client.mixins.json","prominence-emi-index-build.client.mixins.json")){
            String text;try(var in=VerifyAttributeTransforms.class.getClassLoader().getResourceAsStream(name)){text=new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);}
            text=text.replace("local.prominence.emilookup.EmiItemLookupGuard",VerifyMixinTransforms.OfflinePlugin.class.getName()).replace("local.prominence.emiindex.EmiIndexBuildGuard",VerifyMixinTransforms.OfflinePlugin.class.getName());
            String offline="offline-"+name;Files.writeString(cfg.resolve(offline),text);configs.add(offline);
        }
        if(optimized){
            String text="{\"required\":true,\"package\":\"local.prominence.work.mixin\",\"compatibilityLevel\":\"JAVA_17\",\"client\":[\"EmiAttributeScopeMixin\",\"AttributeCandidateIterationMixin\"],\"injectors\":{\"defaultRequire\":1}}";Files.writeString(cfg.resolve("attribute.mixins.json"),text);configs.add("attribute.mixins.json");
        }
        var urls=new ArrayList<URL>();urls.add(cfg.toUri().toURL());for(String p:System.getProperty("java.class.path").split(File.pathSeparator))urls.add(Path.of(p).toUri().toURL());var resources=new URLClassLoader(urls.toArray(URL[]::new),VerifyAttributeTransforms.class.getClassLoader());
        Field field=VerifyMixinTransforms.class.getDeclaredField("LOADER");field.setAccessible(true);field.set(null,resources);MixinBootstrap.init();Method phase=MixinEnvironment.class.getDeclaredMethod("gotoPhase",MixinEnvironment.Phase.class);phase.setAccessible(true);phase.invoke(null,MixinEnvironment.Phase.INIT);MixinExtrasBootstrap.init();MixinEnvironment.getCurrentEnvironment().setObfuscationContext("named:intermediary");
        for(String name:configs)Mixins.addConfiguration(name);for(Config c:Mixins.getConfigs()){c.getConfig().decorate(FabricUtil.KEY_MOD_ID,"prominence_korean_compat");c.getConfig().decorate(FabricUtil.KEY_COMPATIBILITY,FabricUtil.COMPATIBILITY_LATEST);}
        phase.invoke(null,MixinEnvironment.Phase.DEFAULT);MixinEnvironment.getCurrentEnvironment().setObfuscationContext("named:intermediary");field=VerifyMixinTransforms.class.getDeclaredField("TRANSFORMER");field.setAccessible(true);var transformer=(IMixinTransformer)field.get(null);
        var defs=new HashMap<String,byte[]>();try(var paths=Files.walk(fixture)){for(Path p:paths.filter(p->p.toString().endsWith(".class")).toList())defs.put(fixture.relativize(p).toString().replace(File.separatorChar,'.').replaceAll("\\.class$",""),Files.readAllBytes(p));}
        int methods=0;boolean oldLookup=false,oldIndex=false,newIndex=false;String nativeApply=null;
        for(String target:List.of("dev.emi.emi.search.EmiSearch","elocindev.customitemattributes.builder.AttributeBuilderCallback")){
            byte[] raw;try(var in=resources.getResourceAsStream(target.replace('.','/')+".class")){raw=in.readAllBytes();}
            var original=new ClassNode();new ClassReader(raw).accept(original,0);byte[] fixed=transformer.transformClassBytes(target,target,raw);var node=new ClassNode();new ClassReader(fixed).accept(node,0);
            if(target.endsWith("AttributeBuilderCallback")){
                nativeApply=calls(original.methods.stream().filter(m->m.name.equals("applyModifiers")).findFirst().orElseThrow());
                if(!nativeApply.equals(calls(node.methods.stream().filter(m->m.name.equals("applyModifiers")).findFirst().orElseThrow())))throw new AssertionError("Native modifier/UUID calculation changed");
            }
            for(var method:node.methods){new Analyzer<BasicValue>(new BasicVerifier()).analyze(node.name,method);methods++;
                for(var insn:method.instructions)if(insn instanceof MethodInsnNode call){oldLookup|=call.owner.equals("local/prominence/emilookup/EmiItemLookupScope");oldIndex|=call.owner.equals("local/prominence/emiindex/EmiIndexBuildScope");newIndex|=call.owner.equals("local/prominence/work/AttributeCandidates");}
            }
            Files.write(output.resolve(target+".class"),fixed);
            if(target.endsWith("AttributeBuilderCallback")){
                node.methods.removeIf(m->m.name.equals("register"));
                for(var method:node.methods)if(!method.name.equals("applyModifiers"))for(var insn:method.instructions)if(insn instanceof MethodInsnNode call&&call.name.equals("applyModifiers")&&call.owner.equals(node.name)){
                    call.owner="workverification/VerifyAttributeTransforms";call.name="record";call.desc="(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V";call.itf=false;
                }
                var writer=new ClassWriter(0);node.accept(writer);defs.put(target,writer.toByteArray());
            }
        }
        if(!oldLookup||!oldIndex||newIndex!=optimized)throw new AssertionError("Cumulative scope hooks missing");
        for(String n:List.of("local.prominence.work.AttributeCandidates","local.prominence.work.AttributeCandidates$Index","local.prominence.emilookup.EmiItemLookupScope","local.prominence.emilookup.EmiItemLookupScope$Scope","local.prominence.emilookup.EmiItemLookupScope$PropertyResolution"))try(var in=resources.getResourceAsStream(n.replace('.','/')+".class")){defs.put(n,in.readAllBytes());}
        Class<?> runner=new Loader(defs).loadClass("fixture.AttributeFixture");Object result=runner.getMethod("run",boolean.class).invoke(null,optimized);
        var report=new com.google.gson.JsonObject();report.addProperty("optimized",optimized);report.addProperty("asmMethods",methods);report.addProperty("oldLookupAndIndexPreserved",true);report.addProperty("nativeApplyModifiersCallsUnchanged",true);report.addProperty("platformAndModifierFactoryProbes",true);report.addProperty("gameStarted",false);report.add("fixture",com.google.gson.JsonParser.parseString(result.toString()));
        Files.writeString(output.resolve("native-report.json"),report.toString());System.out.println(report);
    }
}
