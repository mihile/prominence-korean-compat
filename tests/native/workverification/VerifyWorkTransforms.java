package workverification;

import batchverification.VerifyMixinTransforms;
import com.llamalad7.mixinextras.MixinExtrasBootstrap;
import java.io.File;
import java.lang.reflect.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import local.prominence.work.InitialWork;
import net.minecraft.class_2960;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.transformer.*;

/** Native bytecode and data-only execution. EMI superclass is a test facade to avoid client/GL bootstrap. */
public final class VerifyWorkTransforms {
    static int keyConversions,registryReads;static class_2960 lastLookup,dynamicId;
    public static String idText(class_2960 id){keyConversions++;return id.toString();}
    public static Object registryLookup(Object registry,class_2960 id){registryReads++;lastLookup=id;return null;}
    public static class_2960 dynamic(){return dynamicId;}
    private static final class NativeLoader extends ClassLoader {
        final Map<String,byte[]> defs;
        NativeLoader(Map<String,byte[]> d){super(VerifyWorkTransforms.class.getClassLoader());defs=d;}
        protected synchronized Class<?> loadClass(String n,boolean resolve)throws ClassNotFoundException{
            Class<?> c=findLoadedClass(n);if(c==null&&defs.containsKey(n)){byte[] b=defs.get(n);c=defineClass(n,b,0,b.length);}if(c==null)c=super.loadClass(n,false);if(resolve)resolveClass(c);return c;
        }
    }
    private static Object allocate(Class<?> type)throws Exception{
        Class<?> u=Class.forName("sun.misc.Unsafe");Field f=u.getDeclaredField("theUnsafe");f.setAccessible(true);return u.getMethod("allocateInstance",Class.class).invoke(f.get(null),type);
    }
    private static void set(Object owner,String name,Object value)throws Exception{Field f=owner.getClass().getDeclaredField(name);f.setAccessible(true);f.set(owner,value);}
    private static byte[] emiFacade(){
        var c=new ClassWriter(0);c.visit(Opcodes.V17,Opcodes.ACC_PUBLIC|Opcodes.ACC_ABSTRACT,"dev/emi/emi/api/stack/EmiStack",null,"java/lang/Object",null);
        var m=c.visitMethod(Opcodes.ACC_PUBLIC,"<init>","()V",null,null);m.visitCode();m.visitVarInsn(Opcodes.ALOAD,0);m.visitMethodInsn(Opcodes.INVOKESPECIAL,"java/lang/Object","<init>","()V",false);m.visitInsn(Opcodes.RETURN);m.visitMaxs(1,1);m.visitEnd();c.visitEnd();return c.toByteArray();
    }
    private static byte[] subclass(String name,boolean dynamic){
        var c=new ClassWriter(0);c.visit(Opcodes.V17,Opcodes.ACC_PUBLIC,name,null,"fzzyhmstrs/emi_loot/util/stack/EmiStackWithTitle",null);
        if(dynamic){var m=c.visitMethod(Opcodes.ACC_PUBLIC,"getId","()Lnet/minecraft/class_2960;",null,null);m.visitCode();m.visitMethodInsn(Opcodes.INVOKESTATIC,"workverification/VerifyWorkTransforms","dynamic","()Lnet/minecraft/class_2960;",false);m.visitInsn(Opcodes.ARETURN);m.visitMaxs(1,1);m.visitEnd();}c.visitEnd();return c.toByteArray();
    }
    public static void main(String[]args)throws Exception{
        Path out=Path.of(args[1]);Files.createDirectories(out);Path cfg=out.resolve("configs");Files.createDirectories(cfg);
        Files.writeString(cfg.resolve("offline-work.mixins.json"),Files.readString(Path.of(args[0])).replace("local.prominence.work.WorkGuard",VerifyMixinTransforms.OfflinePlugin.class.getName()));
        Files.writeString(cfg.resolve("offline-old-key.mixins.json"),"{\"required\":true,\"package\":\"local.prominence.emiindex.mixin\",\"compatibilityLevel\":\"JAVA_17\",\"client\":[\"LootStackKeyMixin\"],\"injectors\":{\"defaultRequire\":1}}");
        Files.writeString(cfg.resolve("offline-old-worker.mixins.json"),"{\"required\":true,\"package\":\"local.prominence.loadcoord.mixin\",\"compatibilityLevel\":\"JAVA_17\",\"client\":[\"EmiWorkerReloadScopeMixin\"],\"injectors\":{\"defaultRequire\":1}}");
        var urls=new ArrayList<URL>();urls.add(cfg.toUri().toURL());for(String p:System.getProperty("java.class.path").split(File.pathSeparator))urls.add(Path.of(p).toUri().toURL());var resources=new URLClassLoader(urls.toArray(URL[]::new),VerifyWorkTransforms.class.getClassLoader());
        Field f=VerifyMixinTransforms.class.getDeclaredField("LOADER");f.setAccessible(true);f.set(null,resources);MixinBootstrap.init();Method phase=MixinEnvironment.class.getDeclaredMethod("gotoPhase",MixinEnvironment.Phase.class);phase.setAccessible(true);phase.invoke(null,MixinEnvironment.Phase.INIT);MixinExtrasBootstrap.init();MixinEnvironment.getCurrentEnvironment().setObfuscationContext("named:intermediary");
        Mixins.addConfiguration("offline-old-key.mixins.json");Mixins.addConfiguration("offline-old-worker.mixins.json");Mixins.addConfiguration("offline-work.mixins.json");
        for(Config config:Mixins.getConfigs()){config.getConfig().decorate(FabricUtil.KEY_MOD_ID,"prominence_korean_compat");config.getConfig().decorate(FabricUtil.KEY_COMPATIBILITY,FabricUtil.COMPATIBILITY_LATEST);}
        phase.invoke(null,MixinEnvironment.Phase.DEFAULT);MixinEnvironment.getCurrentEnvironment().setObfuscationContext("named:intermediary");f=VerifyMixinTransforms.class.getDeclaredField("TRANSFORMER");f.setAccessible(true);var transformer=(IMixinTransformer)f.get(null);
        var defs=new HashMap<String,byte[]>();int methods=0;boolean oldKey=false,oldWorker=false,scope=false,budget=false,parsed=false;
        for(String target:List.of("dev.emi.emi.runtime.EmiReloadManager$ReloadWorker","me.jellysquid.mods.sodium.client.render.chunk.compile.executor.ChunkBuilder","fzzyhmstrs.emi_loot.util.stack.EmiStackWithTitle","elocindev.customitemattributes.api.ItemProperty")){
            byte[] raw;try(var in=resources.getResourceAsStream(target.replace('.','/')+".class")){raw=in.readAllBytes();}byte[] fixed=transformer.transformClassBytes(target,target,raw);if(Arrays.equals(raw,fixed))throw new AssertionError("Missing transform "+target);
            var node=new ClassNode();new ClassReader(fixed).accept(node,0);
            for(var m:node.methods){new Analyzer<BasicValue>(new BasicVerifier()).analyze(node.name,m);methods++;
                for(var i:m.instructions.toArray()){
                    if(i instanceof MethodInsnNode call){
                        oldKey|=call.owner.equals("local/prominence/emiindex/EmiIndexBuildScope");oldWorker|=call.owner.equals("local/prominence/loadcoord/EmiReloadTracker");scope|=call.owner.equals("local/prominence/work/InitialWork")&&call.name.equals("begin");budget|=call.owner.equals("local/prominence/work/InitialWork")&&call.name.equals("budget");parsed|=call.owner.equals("local/prominence/work/ParsedItemId")&&call.name.equals("resolve");
                        if(target.endsWith("EmiStackWithTitle")&&call.owner.equals("net/minecraft/class_2960")&&call.name.equals("toString")){call.setOpcode(Opcodes.INVOKESTATIC);call.owner="workverification/VerifyWorkTransforms";call.name="idText";call.desc="(Lnet/minecraft/class_2960;)Ljava/lang/String;";call.itf=false;}
                        if(target.endsWith("ItemProperty")&&m.name.equals("getItem")&&call.owner.equals("net/minecraft/class_7922")&&call.name.equals("method_10223")){call.setOpcode(Opcodes.INVOKESTATIC);call.owner="workverification/VerifyWorkTransforms";call.name="registryLookup";call.desc="(Ljava/lang/Object;Lnet/minecraft/class_2960;)Ljava/lang/Object;";call.itf=false;}
                    }
                    if(target.endsWith("ItemProperty")&&m.name.equals("getItem")&&i instanceof FieldInsnNode fld&&fld.getOpcode()==Opcodes.GETSTATIC&&fld.owner.equals("net/minecraft/class_7923"))m.instructions.set(i,new InsnNode(Opcodes.ACONST_NULL));
                }
            }
            Files.write(out.resolve(target+".class"),fixed);var writer=new ClassWriter(0);node.accept(writer);defs.put(target,writer.toByteArray());
        }
        if(!oldKey||!oldWorker||!scope||!budget||!parsed)throw new AssertionError("New/native cumulative hooks missing");
        String queueName="me.jellysquid.mods.sodium.client.render.chunk.compile.executor.ChunkJobQueue";
        try(var in=resources.getResourceAsStream(queueName.replace('.','/')+".class")){defs.put(queueName,in.readAllBytes());}
        defs.put("dev.emi.emi.api.stack.EmiStack",emiFacade());defs.put("workfixture.StableLoot",subclass("workfixture/StableLoot",false));defs.put("workfixture.DynamicLoot",subclass("workfixture/DynamicLoot",true));
        var loader=new NativeLoader(defs);Class<?> base=loader.loadClass("fzzyhmstrs.emi_loot.util.stack.EmiStackWithTitle");Object stable=allocate(loader.loadClass("workfixture.StableLoot"));Field id=base.getDeclaredField("id");id.setAccessible(true);id.set(stable,new class_2960("example:chests/home"));var key=base.getMethod("getKey");
        Method previousKey=Arrays.stream(base.getDeclaredMethods()).filter(m->m.getName().startsWith("getKey$mixinextras$wrapped$")&&m.getParameterCount()==0).findFirst().orElseThrow();previousKey.setAccessible(true);
        Class<?> oldScopes=Class.forName("local.prominence.emiindex.EmiIndexBuildScope");Object previousScope=oldScopes.getMethod("begin",String.class).invoke(null,"previous-key-fixture");
        for(int i=0;i<1_000_000;i++)if(!previousKey.invoke(stable).equals("example:chests/home"))throw new AssertionError("Previous native key changed");
        Field requests=previousScope.getClass().getDeclaredField("keyRequests");requests.setAccessible(true);long previousQueries=requests.getLong(previousScope);previousScope.getClass().getMethod("close",boolean.class).invoke(previousScope,true);
        if(previousQueries!=1_000_000||keyConversions!=1)throw new AssertionError("Baseline must preserve previous text cache");
        keyConversions=0;Object optimizedScope=oldScopes.getMethod("begin",String.class).invoke(null,"optimized-key-fixture");Object nativeKey=key.invoke(stable);for(int i=0;i<1_000_000;i++)if(key.invoke(stable)!=nativeKey)throw new AssertionError("Stable key not reused");long optimizedQueries=requests.getLong(optimizedScope);optimizedScope.getClass().getMethod("close",boolean.class).invoke(optimizedScope,true);
        if(optimizedQueries!=1||keyConversions!=1)throw new AssertionError("Native key repeated scope lookups "+optimizedQueries);
        Object dynamic=allocate(loader.loadClass("workfixture.DynamicLoot"));id.set(dynamic,new class_2960("example:fixed"));dynamicId=new class_2960("example:a");if(!key.invoke(dynamic).equals("example:a"))throw new AssertionError("Dynamic subclass lost");dynamicId=new class_2960("example:b");if(!key.invoke(dynamic).equals("example:b"))throw new AssertionError("Dynamic ID cached");
        Class<?> property=loader.loadClass("elocindev.customitemattributes.api.ItemProperty");Object p=property.getConstructor(String.class,List.class,List.class).newInstance(new String("minecraft:stone"),List.of(),List.of());Method getItem=property.getMethod("getItem");getItem.invoke(p);var first=lastLookup;
        for(int i=0;i<1000;i++){getItem.invoke(p);if(lastLookup!=first)throw new AssertionError("Native parse repeated");}if(registryReads!=1001)throw new AssertionError("Registry result cached");property.getField("item").set(p,"minecraft:dirt");getItem.invoke(p);if(lastLookup==first||!lastLookup.toString().equals("minecraft:dirt"))throw new AssertionError("Config ID not updated");
        var gson=new com.google.gson.Gson();Object decoded=gson.fromJson("{\"item\":\"minecraft:stone\",\"affected_slots\":[],\"attribute_overrides\":[]}",property);getItem.invoke(decoded);var gsonId=lastLookup;getItem.invoke(decoded);if(lastLookup!=gsonId||!gsonId.toString().equals("minecraft:stone"))throw new AssertionError("Gson constructor bypass unsupported");if(gson.toJson(decoded).contains("prominence$"))throw new AssertionError("Runtime cache leaked into configuration JSON");
        Class<?> builder=loader.loadClass("me.jellysquid.mods.sodium.client.render.chunk.compile.executor.ChunkBuilder");Object b=allocate(builder);var threads=new ArrayList<Thread>();for(int i=0;i<10;i++)threads.add(new Thread());set(b,"threads",threads);set(b,"busyThreadCount",new AtomicInteger(2));Class<?> queue=loader.loadClass("me.jellysquid.mods.sodium.client.render.chunk.compile.executor.ChunkJobQueue");var ctor=queue.getDeclaredConstructor();ctor.setAccessible(true);set(b,"queue",ctor.newInstance());Method getBudget=builder.getMethod("getSchedulingBudget");if(!getBudget.invoke(b).equals(10))throw new AssertionError("Native idle scheduling changed");try(var work=InitialWork.begin()){if(!getBudget.invoke(b).equals(3))throw new AssertionError("Native running tasks not respected");}if(!getBudget.invoke(b).equals(10))throw new AssertionError("Scheduling not restored");
        String report="{\"status\":\"passed\",\"targets\":4,\"methods\":"+methods+",\"stableNativeKeyCalls\":1000001,\"previousScopeQueries\":"+previousQueries+",\"optimizedScopeQueries\":"+optimizedQueries+",\"stableKeyConversionsBoth\":1,\"dynamicGetterPreserved\":true,\"nativePropertyLookups\":"+registryReads+",\"registryLookupEveryTime\":true,\"nativeTerrainBudgetRestored\":true,\"previousKeyAndReloadWrappersPreserved\":true,\"gameStarted\":false,\"workerThreadsStarted\":false,\"emiFacadeUsedForDataOnlyKeyExecution\":true}";Files.writeString(out.resolve("native-report.json"),report);System.out.println(report);
    }
}
