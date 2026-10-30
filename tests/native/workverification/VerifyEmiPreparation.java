package workverification;

import batchverification.VerifyMixinTransforms;
import com.llamalad7.mixinextras.MixinExtrasBootstrap;
import dev.emi.emi.api.stack.*;
import java.io.File;
import java.lang.reflect.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import local.prominence.work.EmiStackBuildScope;
import net.minecraft.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.transformer.*;

/** Executes installed anvil/predicate method bodies; Minecraft/item-stack boundaries are data facades. */
public final class VerifyEmiPreparation {
    static int checks;
    static void check(boolean b,String message){checks++;if(!b)throw new AssertionError(message);}
    public static final class CustomDefault extends class_1792 {
        public class_1799 method_7854(){var s=new class_1799(this);s.method_7978(new class_1887("innate:first"),2);return s;}
    }
    static final class Loader extends ClassLoader {
        final Map<String,byte[]> defs;
        Loader(Map<String,byte[]> defs){super(VerifyEmiPreparation.class.getClassLoader());this.defs=defs;}
        protected synchronized Class<?> loadClass(String n,boolean resolve)throws ClassNotFoundException{
            Class<?> c=findLoadedClass(n);if(c==null&&defs.containsKey(n)){byte[] b=defs.get(n);c=defineClass(n,b,0,b.length);}if(c==null)c=super.loadClass(n,false);if(resolve)resolveClass(c);return c;
        }
    }
    static byte[] dataMethods(byte[] raw,Set<String> methods){
        var n=new ClassNode();new ClassReader(raw).accept(n,0);n.interfaces.clear();n.methods.removeIf(m->!methods.contains(m.name));var w=new ClassWriter(0);n.accept(w);return w.toByteArray();
    }
    static String value(EmiStack s){return (s.getKey()==class_1802.field_8598?"book":System.identityHashCode(s.getKey()))+":"+s.getAmount()+":"+(s.getNbt()==null?"null":s.getNbt().map);}
    static Object recipe(Class<?> type,class_1792 item)throws Exception{return type.getConstructor(class_1792.class,class_1887.class,int.class,class_2960.class).newInstance(item,new class_1887("native:looting"),3,null);}
    static List<String> read(Object r)throws Exception{
        var out=new ArrayList<String>();for(String name:List.of("getInputs","getOutputs"))for(Object s:(List<?>)r.getClass().getMethod(name).invoke(r))out.add(value((EmiStack)s));return out;
    }
    public static void main(String[] args)throws Exception{
        Path out=Path.of(args[1]);Files.createDirectories(out);Path cfg=out.resolve("configs");Files.createDirectories(cfg);
        Files.writeString(cfg.resolve("preparation.mixins.json"),Files.readString(Path.of(args[0])).replace("local.prominence.work.EmiPreparationGuard",VerifyMixinTransforms.OfflinePlugin.class.getName()));
        Files.writeString(cfg.resolve("previous-index.mixins.json"),"{\"required\":true,\"package\":\"local.prominence.emiindex.mixin\",\"compatibilityLevel\":\"JAVA_17\",\"client\":[\"EmiRecipeIndexScopeMixin\",\"EmiRecipeWorkerIndexScopeMixin\"],\"injectors\":{\"defaultRequire\":1}}");
        var urls=new ArrayList<URL>();urls.add(cfg.toUri().toURL());for(String s:System.getProperty("java.class.path").split(File.pathSeparator))urls.add(Path.of(s).toUri().toURL());urls.removeIf(u->u.getPath().endsWith("/fixture/"));var resources=new URLClassLoader(urls.toArray(URL[]::new),VerifyEmiPreparation.class.getClassLoader()){public URL getResource(String n){URL local=findResource(n);return local==null?super.getResource(n):local;}};
        Field f=VerifyMixinTransforms.class.getDeclaredField("LOADER");f.setAccessible(true);f.set(null,resources);MixinBootstrap.init();var phase=MixinEnvironment.class.getDeclaredMethod("gotoPhase",MixinEnvironment.Phase.class);phase.setAccessible(true);phase.invoke(null,MixinEnvironment.Phase.INIT);MixinExtrasBootstrap.init();MixinEnvironment.getCurrentEnvironment().setObfuscationContext("named:intermediary");
        Mixins.addConfiguration("previous-index.mixins.json");Mixins.addConfiguration("preparation.mixins.json");for(Config c:Mixins.getConfigs()){c.getConfig().decorate(FabricUtil.KEY_MOD_ID,"prominence_korean_compat");c.getConfig().decorate(FabricUtil.KEY_COMPATIBILITY,FabricUtil.COMPATIBILITY_LATEST);}phase.invoke(null,MixinEnvironment.Phase.DEFAULT);MixinEnvironment.getCurrentEnvironment().setObfuscationContext("named:intermediary");f=VerifyMixinTransforms.class.getDeclaredField("TRANSFORMER");f.setAccessible(true);var transformer=(IMixinTransformer)f.get(null);
        var oldDefs=new HashMap<String,byte[]>();var newDefs=new HashMap<String,byte[]>();int methods=0;
        String anvil="dev.emi.emi.recipe.special.EmiAnvilEnchantRecipe",hidden="elocindev.prominent.plugin.ProminentEMIPlugin";
        for(String target:List.of(anvil,hidden,"dev.emi.emi.registry.EmiRecipes","dev.emi.emi.registry.EmiRecipes$Worker")){
            byte[] raw;try(var in=resources.getResourceAsStream(target.replace('.','/')+".class")){raw=in.readAllBytes();}byte[] fixed=transformer.transformClassBytes(target,target,raw);check(!Arrays.equals(raw,fixed),"Missing injection "+target);
            var node=new ClassNode();new ClassReader(fixed).accept(node,0);for(var m:node.methods){new Analyzer<BasicValue>(new BasicVerifier()).analyze(node.name,m);methods++;}
            Files.write(out.resolve(target+".class"),fixed);
            if(target.equals(anvil)){
                var keep=new HashSet<>(List.of("<init>","getInputs","getOutputs","getBook","getTool"));oldDefs.put(target,dataMethods(raw,keep));
                for(var m:node.methods)if(m.name.contains("prominence$")||m.name.contains("mixinextras$")||m.name.startsWith("getBook$"))keep.add(m.name);
                newDefs.put(target,dataMethods(fixed,keep));
            } else if(target.equals(hidden)){
                var keep=new HashSet<>(List.of("<init>","lambda$initialize$0"));oldDefs.put(target,dataMethods(raw,keep));for(var m:node.methods)if(m.name.contains("prominence$")||m.name.contains("mixinextras$")||m.name.startsWith("lambda$initialize$0$"))keep.add(m.name);newDefs.put(target,dataMethods(fixed,keep));
            }
        }
        var oldLoader=new Loader(oldDefs);var newLoader=new Loader(newDefs);Class<?> oldAnvil=oldLoader.loadClass(anvil),newAnvil=newLoader.loadClass(anvil);
        var item=new class_1792();Object original=recipe(oldAnvil,item),patched=recipe(newAnvil,item);List<String> expected=read(original);
        class_1799.constructions=0;for(int i=0;i<1000;i++)check(read(original).equals(expected),"Baseline changed");int oldCount=class_1799.constructions;
        class_1799.constructions=0;try(var scope=EmiStackBuildScope.begin("native-anvil")){
            for(int i=0;i<1000;i++)check(read(patched).equals(expected),"Native output changed");
            var inputs=(List<?>)newAnvil.getMethod("getInputs").invoke(patched);((ItemEmiStack)inputs.get(0)).amount=9;((EmiStack)inputs.get(1)).getNbt().map.clear();check(read(patched).equals(expected),"Caller mutation poisoned private templates");
            try(var nested=EmiStackBuildScope.begin("nested")){check(read(patched).equals(expected),"Nested scope");}
            check(read(patched).equals(expected),"Parent scope restored");
        }int newCount=class_1799.constructions;check(newCount<oldCount/20,"No useful native allocation reduction: "+oldCount+" -> "+newCount);
        class_1799.constructions=0;read(patched);check(class_1799.constructions>0,"Scope leaked beyond close");
        var custom=new CustomDefault();var a=recipe(oldAnvil,custom);var b=recipe(newAnvil,custom);var customExpected=read(a);check(customExpected.toString().contains("innate:first"),"Fixture must test original first enchantment semantics");try(var scope=EmiStackBuildScope.begin("custom-default")){for(int i=0;i<20;i++)check(read(b).equals(customExpected),"Custom default/innate enchantment changed");}
        try{try(var scope=EmiStackBuildScope.begin("exception")){throw new IllegalStateException();}}catch(IllegalStateException expectedException){}class_1799.constructions=0;read(patched);check(class_1799.constructions>0,"Exceptional scope leaked");
        var nbtTool=new class_1792();class_1799.nbtItems.add(nbtTool);var nbtRecipe=recipe(newAnvil,nbtTool);
        try(var scope=EmiStackBuildScope.begin("constructor-nbt")){var first=read(nbtRecipe);var second=read(nbtRecipe);check(!first.equals(second),"Constructor NBT was frozen");}class_1799.nbtItems.clear();
        try(var scope=EmiStackBuildScope.begin("thread-owner")){read(patched);class_1799.constructions=0;Thread t=new Thread(()->{try{read(patched);}catch(Exception e){throw new AssertionError(e);}});t.start();t.join();check(class_1799.constructions>=4,"Another thread inherited private templates");}
        var oldPred=oldLoader.loadClass(hidden).getDeclaredMethod("lambda$initialize$0",EmiStack.class);oldPred.setAccessible(true);var newPred=newLoader.loadClass(hidden).getDeclaredMethod("lambda$initialize$0",EmiStack.class);newPred.setAccessible(true);
        for(class_1792 i:List.of(item,elocindev.prominent.registry.ItemRegistry.SKILLET,class_1802.field_8162))for(long count:new long[]{1,64,0,-1,Integer.MAX_VALUE,(long)Integer.MAX_VALUE+1,4294967297L}){
            var stack=new ItemEmiStack(i,null,count);check(oldPred.invoke(null,stack).equals(newPred.invoke(null,stack)),"Hidden predicate count/empty mismatch");
        }
        EmiStack customStack=new EmiStack(){public class_1799 getItemStack(){return new class_1799(elocindev.prominent.registry.ItemRegistry.SKILLET);}};check(oldPred.invoke(null,customStack).equals(newPred.invoke(null,customStack)),"Custom stack must use original predicate");
        var skillet=new ItemEmiStack(elocindev.prominent.registry.ItemRegistry.SKILLET,null,1);class_1799.constructions=0;for(int i=0;i<10000;i++)oldPred.invoke(null,skillet);int oldHidden=class_1799.constructions;class_1799.constructions=0;for(int i=0;i<10000;i++)newPred.invoke(null,skillet);check(class_1799.constructions==0&&oldHidden==10000,"Hidden item still creates stacks");
        String report="{\"checks\":"+checks+",\"targets\":4,\"methods\":"+methods+",\"anvilOriginalStackConstructions\":"+oldCount+",\"anvilPatchedStackConstructions\":"+newCount+",\"hiddenOriginalStackConstructions\":"+oldHidden+",\"hiddenPatchedStackConstructions\":0,\"minecraftBoundariesAreFacades\":true,\"gameStarted\":false}";Files.writeString(out.resolve("report.json"),report);System.out.println(report);
    }
}
