package batchverification;

import com.google.gson.GsonBuilder;
import com.llamalad7.mixinextras.MixinExtrasBootstrap;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import local.prominence.memory.VariantNbtRewrite;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.BasicVerifier;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.launch.platform.container.ContainerHandleVirtual;
import org.spongepowered.asm.launch.platform.container.IContainerHandle;
import org.spongepowered.asm.logging.ILogger;
import org.spongepowered.asm.logging.LoggerAdapterConsole;
import org.spongepowered.asm.mixin.FabricUtil;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.transformer.Config;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;
import org.spongepowered.asm.mixin.transformer.IMixinTransformerFactory;
import org.spongepowered.asm.service.IAdviceProvider;
import org.spongepowered.asm.service.IClassBytecodeProvider;
import org.spongepowered.asm.service.IClassProvider;
import org.spongepowered.asm.service.IClassTracker;
import org.spongepowered.asm.service.IFeatureValidator;
import org.spongepowered.asm.service.IGlobalPropertyService;
import org.spongepowered.asm.service.IMixinAuditTrail;
import org.spongepowered.asm.service.IMixinInternal;
import org.spongepowered.asm.service.IMixinService;
import org.spongepowered.asm.service.IMixinServiceBootstrap;
import org.spongepowered.asm.service.IPropertyKey;
import org.spongepowered.asm.service.ITransformer;
import org.spongepowered.asm.service.ITransformerProvider;
import org.spongepowered.asm.util.Constants;
import org.spongepowered.asm.util.ReEntranceLock;

public final class VerifyMixinTransforms
        implements IMixinService, IClassProvider, IClassBytecodeProvider, ITransformerProvider, IClassTracker {

    public static final class Bootstrap implements IMixinServiceBootstrap {
        @Override
        public String getName() {
            return "OfflineMixinVerifier";
        }

        @Override
        public String getServiceClassName() {
            return VerifyMixinTransforms.class.getName();
        }

        @Override
        public void bootstrap() {}
    }

    public static final class Props implements IGlobalPropertyService {
        private static final Map<IPropertyKey, Object> MAP = new HashMap<>();

        private record Key(String name) implements IPropertyKey {}

        @Override
        public IPropertyKey resolveKey(String name) {
            return new Key(name);
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> T getProperty(IPropertyKey key) {
            return (T) MAP.get(key);
        }

        @Override
        public void setProperty(IPropertyKey key, Object value) {
            MAP.put(key, value);
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> T getProperty(IPropertyKey key, T defaultValue) {
            return (T) MAP.getOrDefault(key, defaultValue);
        }

        @Override
        public String getPropertyString(IPropertyKey key, String defaultValue) {
            Object v = MAP.get(key);
            return v != null ? v.toString() : defaultValue;
        }
    }

    public static final class OfflinePlugin implements IMixinConfigPlugin {
        static volatile boolean extraSpellPostApplyCalled;
        static volatile boolean extraSpellPostApplyRewritten;
        static final List<String> appliedMixins = Collections.synchronizedList(new ArrayList<>());

        @Override
        public void onLoad(String mixinPackage) {}

        @Override
        public String getRefMapperConfig() {
            return null;
        }

        @Override
        public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
            return true;
        }

        @Override
        public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

        @Override
        public List<String> getMixins() {
            return null;
        }

        @Override
        public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

        @Override
        public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
            appliedMixins.add(mixinClassName); if(mixinClassName.endsWith("ArmorLayerTrackingMixin"))auditArmor(targetClass);
            if (mixinClassName.endsWith("VariantRendererMarkerMixin")) {
                extraSpellPostApplyCalled = true;
                extraSpellPostApplyRewritten = auditVariant(targetClass);
            }
        }
    }

    private static URLClassLoader LOADER;
    private static IMixinTransformer TRANSFORMER;
    private final ReEntranceLock lock = new ReEntranceLock(1);

    @Override
    public String getName() {
        return "OfflineMixinVerifier";
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public void prepare() {}

    @Override
    public MixinEnvironment.Phase getInitialPhase() {
        return MixinEnvironment.Phase.PREINIT;
    }

    @Override
    public void offer(IMixinInternal internal) {
        if (internal instanceof IMixinTransformerFactory factory && TRANSFORMER == null) {
            TRANSFORMER = factory.createTransformer();
        }
    }

    @Override
    public void init() {}

    @Override
    public void beginPhase() {}

    @Override
    public void checkEnv(Object bootSource) {}

    @Override
    public ReEntranceLock getReEntranceLock() {
        return lock;
    }

    @Override
    public IClassProvider getClassProvider() {
        return this;
    }

    @Override
    public IClassBytecodeProvider getBytecodeProvider() {
        return this;
    }

    @Override
    public ITransformerProvider getTransformerProvider() {
        return this;
    }

    @Override
    public IClassTracker getClassTracker() {
        return this;
    }

    @Override
    public IMixinAuditTrail getAuditTrail() {
        return null;
    }

    @Override
    public IFeatureValidator getFeatureValidator() {
        return IFeatureValidator.ALLOW_ALL;
    }

    @Override
    public IAdviceProvider getAdviceProvider() {
        return IAdviceProvider.GENERIC;
    }

    @Override
    public Collection<String> getPlatformAgents() {
        return List.of("org.spongepowered.asm.launch.platform.MixinPlatformAgentDefault");
    }

    @Override
    public IContainerHandle getPrimaryContainer() {
        return new ContainerHandleVirtual(getName());
    }

    @Override
    public Collection<IContainerHandle> getMixinContainers() {
        return Collections.emptyList();
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        return LOADER.getResourceAsStream(name);
    }

    @Override
    public String getSideName() {
        return Constants.SIDE_CLIENT;
    }

    @Override
    public MixinEnvironment.CompatibilityLevel getMinCompatibilityLevel() {
        return MixinEnvironment.CompatibilityLevel.JAVA_8;
    }

    @Override
    public MixinEnvironment.CompatibilityLevel getMaxCompatibilityLevel() {
        return MixinEnvironment.CompatibilityLevel.JAVA_21;
    }

    @Override
    public ILogger getLogger(String name) {
        return new LoggerAdapterConsole(name);
    }

    @Override
    public URL[] getClassPath() {
        return LOADER.getURLs();
    }

    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
        return Class.forName(name, false, LOADER);
    }

    @Override
    public Class<?> findClass(String name, boolean initialize) throws ClassNotFoundException {
        return Class.forName(name, initialize, LOADER);
    }

    @Override
    public Class<?> findAgentClass(String name, boolean initialize) throws ClassNotFoundException {
        return Class.forName(name, initialize, VerifyMixinTransforms.class.getClassLoader());
    }

    @Override
    public ClassNode getClassNode(String name) throws ClassNotFoundException, IOException {
        return getClassNode(name, true, 0);
    }

    @Override
    public ClassNode getClassNode(String name, boolean runTransformers) throws ClassNotFoundException, IOException {
        return getClassNode(name, runTransformers, 0);
    }

    @Override
    public ClassNode getClassNode(String name, boolean runTransformers, int readerFlags)
            throws ClassNotFoundException, IOException {
        String res = name.replace('.', '/') + ".class";
        try (InputStream in = LOADER.getResourceAsStream(res)) {
            if (in == null) {
                throw new ClassNotFoundException(name);
            }
            ClassNode cn = new ClassNode();
            new ClassReader(in.readAllBytes()).accept(cn, readerFlags);
            return cn;
        }
    }

    @Override
    public Collection<ITransformer> getTransformers() {
        return Collections.emptyList();
    }

    @Override
    public Collection<ITransformer> getDelegatedTransformers() {
        return Collections.emptyList();
    }

    @Override
    public void addTransformerExclusion(String name) {}

    @Override
    public void registerInvalidClass(String className) {}

    @Override
    public boolean isClassLoaded(String className) {
        return false;
    }

    @Override
    public String getClassRestrictions(String className) {
        return "";
    }

    private static int rewrites,verified;
    private static long armorFields;
    private static void auditArmor(ClassNode node) {
        armorFields=node.fields.stream().filter(f->f.name.contains("gl_storedEntity")&&f.desc.equals("Lnet/minecraft/class_1309;")).count();
        if(armorFields!=2)throw new AssertionError("Both native armor owner fields must be present");
    }
    private static final Map<String,byte[]> FIXED=new LinkedHashMap<>();
    private static byte[] encode(ClassNode node){var writer=new org.objectweb.asm.ClassWriter(0);node.accept(writer);return writer.toByteArray();}
    private static boolean auditVariant(ClassNode target) {
        byte[] before=encode(target);
        var candidates=new ArrayList<MethodInsnNode>();var metadata=new ArrayList<Object[]>();
        for(var m:target.methods)for(var i:m.instructions)if(i instanceof MethodInsnNode call&&call.owner.startsWith("net/minecraft/")&&(call.name.equals("method_5647")||call.name.equals("method_5652"))) {
            candidates.add(call);metadata.add(new Object[]{call.getOpcode(),call.owner,call.name,call.desc,call.itf});
        }
        if(VariantNbtRewrite.rewrite(target)!=1)throw new AssertionError("MMV callback shape "+target.name);
        int changed=0;
        for(int n=0;n<candidates.size();n++)if(candidates.get(n).owner.equals("local/prominence/memory/VariantProjection")) {
            changed++;var call=candidates.get(n);var old=metadata.get(n);
            call.setOpcode((Integer)old[0]);call.owner=(String)old[1];call.name=(String)old[2];call.desc=(String)old[3];call.itf=(Boolean)old[4];
        }
        if(changed!=1||!Arrays.equals(before,encode(target)))throw new AssertionError("MMV logic other than the one NBT call changed");
        if(VariantNbtRewrite.rewrite(target)!=1||VariantNbtRewrite.rewrite(target)!=0)throw new AssertionError("Rewrite repeatability");
        rewrites++;return true;
    }
    private static List<String> calls(MethodNode method){
        var out=new ArrayList<String>();for(var i:method.instructions)if(i instanceof MethodInsnNode m)out.add(m.owner+"."+m.name+m.desc);return out;
    }
    public static final class Clock {public static long time;public static long now(){return time;}}
    public static final class FootstepLog {public static final org.apache.logging.log4j.Logger logger=org.apache.logging.log4j.LogManager.getLogger("offline-footsteps");}
    public static class World {}
    public static final class ClientWorld extends World {}
    public static final class Entity {final World world;public Entity(World world){this.world=world;}public World method_37908(){return world;}}
    private static int behaviorChecks;
    private static void check(boolean value,String text){behaviorChecks++;if(!value)throw new AssertionError(text);}
    private static final class FixtureLoader extends ClassLoader {
        final Map<String,byte[]> definitions;
        FixtureLoader(Map<String,byte[]> defs){super(VerifyMixinTransforms.class.getClassLoader());definitions=defs;}
        protected synchronized Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException {
            Class<?> c=findLoadedClass(name);
            if(c==null&&definitions.containsKey(name)){byte[] b=definitions.get(name);c=defineClass(name,b,0,b.length);}
            if(c==null)c=super.loadClass(name,false);if(resolve)resolveClass(c);return c;
        }
    }
    private static Object options(float delay,boolean delayed)throws Exception {
        Class<?> api=Class.forName("eu.ha3.presencefootsteps.sound.Options",false,VerifyMixinTransforms.class.getClassLoader());
        return java.lang.reflect.Proxy.newProxyInstance(api.getClassLoader(),new Class<?>[]{api},(p,m,a)->switch(m.getName()){
            case "containsKey"->delayed&&(a[0].equals("delay_min")||a[0].equals("delay_max"));
            case "get","getOrDefault"->delay;case "toString"->"offline-options";default->null;});
    }
    private static Object delayed(boolean fixed,List<String> played)throws Exception {
        String prefix="eu.ha3.presencefootsteps.sound.player.DelayedSoundPlayer";
        var url=LOADER.getResource(prefix.replace('.','/')+".class");
        var jar=((java.net.JarURLConnection)url.openConnection()).getJarFile();Map<String,byte[]> defs=new HashMap<>();
        var entries=jar.entries();while(entries.hasMoreElements()) {
            String path=entries.nextElement().getName();if(!path.startsWith(prefix.replace('.','/'))||!path.endsWith(".class"))continue;
            String name=path.substring(0,path.length()-6).replace('/','.');byte[] b;
            if(fixed&&FIXED.containsKey(name))b=FIXED.get(name);else try(var in=LOADER.getResourceAsStream(path)){b=in.readAllBytes();}
            ClassNode node=new ClassNode();new ClassReader(b).accept(node,0);
            for(var m:node.methods)for(var i:m.instructions) {
                if(i instanceof MethodInsnNode call&&call.owner.equals("java/lang/System")&&call.name.equals("currentTimeMillis")){call.owner="batchverification/VerifyMixinTransforms$Clock";call.name="now";}
                if(i instanceof org.objectweb.asm.tree.FieldInsnNode field&&field.owner.equals("eu/ha3/presencefootsteps/PresenceFootsteps")&&field.name.equals("logger"))field.owner="batchverification/VerifyMixinTransforms$FootstepLog";
            }
            defs.put(name,encode(node));
        }
        Class<?> api=Class.forName("eu.ha3.presencefootsteps.sound.player.SoundPlayer",false,VerifyMixinTransforms.class.getClassLoader());
        Object immediate=java.lang.reflect.Proxy.newProxyInstance(api.getClassLoader(),new Class<?>[]{api},(p,m,a)->{
            if(m.getName().equals("getRNG"))return new java.util.Random(5);
            if(m.getName().equals("playSound")){check(((Float)a[2])==0.5f&&((Float)a[3])==0.75f,"Sound volume/pitch changed");played.add((String)a[1]);}return null;
        });
        Class<?> type=new FixtureLoader(defs).loadClass(prefix);return type.getConstructor(api).newInstance(immediate);
    }
    private static void play(Object queue,String name,float delay,boolean useDelay)throws Exception {
        var method=Arrays.stream(queue.getClass().getMethods()).filter(m->m.getName().equals("playSound")).findFirst().orElseThrow();
        method.invoke(queue,new Object[]{null,name,0.5f,0.75f,options(delay,useDelay)});
    }
    private static int pending(Object queue)throws Exception {var f=queue.getClass().getDeclaredField("pending");f.setAccessible(true);return ((List<?>)f.get(queue)).size();}
    private static long due(Object queue,int index)throws Exception {var f=queue.getClass().getDeclaredField("pending");f.setAccessible(true);Object sound=((List<?>)f.get(queue)).get(index);var time=sound.getClass().getDeclaredField("timeToPlay");time.setAccessible(true);return time.getLong(sound);}
    private static void think(Object queue)throws Exception {queue.getClass().getMethod("think").invoke(queue);}
    private static void footstepProbes()throws Exception {
        long base=1_700_000_000_000L;List<String> sounds=new ArrayList<>();Clock.time=base;Object old=delayed(false,sounds);
        play(old,"old",100,true);Clock.time=base+100;think(old);
        check(sounds.equals(List.of("old"))&&pending(old)==1,"Native played-entry retention was not reproduced");
        Clock.time=base+110;play(old,"stuck",100,true);Clock.time=base+10_000;think(old);
        check(sounds.equals(List.of("old")),"Native sentinel blockage was not reproduced");

        sounds.clear();Clock.time=base;Object fixed=delayed(true,sounds);play(fixed,"one",100,true);
        check(due(fixed,0)==base+100,"First schedule changed");Clock.time=base+10;play(fixed,"two",200,true);
        check(due(fixed,1)==base+210,"Absolute time added twice");Clock.time=base+50;think(fixed);check(sounds.isEmpty(),"Played before allowed delay");
        Clock.time=base+100;think(fixed);check(sounds.equals(List.of("one"))&&pending(fixed)==1,"Played entry not consumed exactly once");
        Clock.time=base+210;think(fixed);check(sounds.equals(List.of("one","two"))&&pending(fixed)==0,"Remaining due sound not scheduled");
        Clock.time=base+220;play(fixed,"after-empty",100,true);check(due(fixed,0)==base+320,"Empty sentinel blocked future enqueue");
        Clock.time=base+320;think(fixed);think(fixed);check(sounds.equals(List.of("one","two","after-empty"))&&pending(fixed)==0,"Sound repeated or pending leaked");
        play(fixed,"immediate",0,false);check(sounds.get(sounds.size()-1).equals("immediate")&&pending(fixed)==0,"Immediate playback changed");
        Clock.time=base+400;play(fixed,"late",100,true);Clock.time=base+5000;think(fixed);check(!sounds.contains("late")&&pending(fixed)==0,"Late-sound skip changed");
    }
    private static Class<?> holderFixture(boolean azure)throws Exception {
        String nativeName=azure?"mod.azure.azurelib.renderer.GeoEntityRenderer":"net.minecraft.class_1799";
        ClassNode nativeNode=new ClassNode();new ClassReader(FIXED.get(nativeName)).accept(nativeNode,0);
        ClassNode fixture=new ClassNode();fixture.version=org.objectweb.asm.Opcodes.V17;fixture.access=org.objectweb.asm.Opcodes.ACC_PUBLIC;
        fixture.name=nativeName.replace('.','/');fixture.superName="java/lang/Object";
        if(azure)fixture.interfaces.add("local/prominence/memory/AzureRendererOwner");
        for(var f:nativeNode.fields)if(azure?f.name.equals("animatable"):f.name.equals("field_24093")||f.name.equals("field_8037")){f.access=org.objectweb.asm.Opcodes.ACC_PUBLIC|((f.access&org.objectweb.asm.Opcodes.ACC_STATIC)!=0?org.objectweb.asm.Opcodes.ACC_STATIC:0);fixture.fields.add(f);}
        for(var m:nativeNode.methods)if(azure?m.name.equals("prominence$clearObsoleteWorld"):m.name.equals("method_27320")||m.name.contains("prominence$keepEmptyUnowned"))fixture.methods.add(m);
        var ctor=new MethodNode(org.objectweb.asm.Opcodes.ACC_PUBLIC,"<init>","()V",null,null);ctor.instructions.add(new org.objectweb.asm.tree.VarInsnNode(org.objectweb.asm.Opcodes.ALOAD,0));ctor.instructions.add(new MethodInsnNode(org.objectweb.asm.Opcodes.INVOKESPECIAL,"java/lang/Object","<init>","()V",false));ctor.instructions.add(new org.objectweb.asm.tree.InsnNode(org.objectweb.asm.Opcodes.RETURN));ctor.maxStack=1;ctor.maxLocals=1;fixture.methods.add(ctor);
        if(!azure){var init=new MethodNode(org.objectweb.asm.Opcodes.ACC_STATIC,"<clinit>","()V",null,null);init.instructions.add(new org.objectweb.asm.tree.TypeInsnNode(org.objectweb.asm.Opcodes.NEW,fixture.name));init.instructions.add(new org.objectweb.asm.tree.InsnNode(org.objectweb.asm.Opcodes.DUP));init.instructions.add(new MethodInsnNode(org.objectweb.asm.Opcodes.INVOKESPECIAL,fixture.name,"<init>","()V",false));init.instructions.add(new org.objectweb.asm.tree.FieldInsnNode(org.objectweb.asm.Opcodes.PUTSTATIC,fixture.name,"field_8037","L"+fixture.name+";"));init.instructions.add(new org.objectweb.asm.tree.InsnNode(org.objectweb.asm.Opcodes.RETURN));init.maxStack=2;fixture.methods.add(init);}
        String generated=azure?"batchverification/runtime/AzureHolder":"batchverification/runtime/EmptyHolder";
        var remapper=new org.objectweb.asm.commons.Remapper(){public String map(String name){
            if(name.equals(nativeName.replace('.','/')))return generated;
            return switch(name){case "net/minecraft/class_1297"->"batchverification/VerifyMixinTransforms$Entity";case "net/minecraft/class_1937"->"batchverification/VerifyMixinTransforms$World";case "net/minecraft/class_638"->"batchverification/VerifyMixinTransforms$ClientWorld";default->name;};
        }};
        var writer=new org.objectweb.asm.ClassWriter(0);fixture.accept(new org.objectweb.asm.commons.ClassRemapper(writer,remapper));
        return new FixtureLoader(Map.of(generated.replace('/','.'),writer.toByteArray())).loadClass(generated.replace('/','.'));
    }
    private static void holderProbes()throws Exception {
        Class<?> azure=holderFixture(true);Object renderer=azure.getConstructor().newInstance();var field=azure.getField("animatable");
        World current=new ClientWorld(),old=new ClientWorld();Entity active=new Entity(current),previous=new Entity(old);field.set(renderer,active);
        var owner=(local.prominence.memory.AzureRendererOwner)renderer;
        check(!owner.prominence$clearObsoleteWorld(current)&&field.get(renderer)==active,"Current Azure render owner cleared");
        field.set(renderer,previous);check(owner.prominence$clearObsoleteWorld(current)&&field.get(renderer)==null,"Previous Azure world held");
        Entity server=new Entity(new World());field.set(renderer,server);check(!owner.prominence$clearObsoleteWorld(current)&&field.get(renderer)==server,"Server-world context touched");
        field.set(renderer,active);check(owner.prominence$clearObsoleteWorld(null)&&field.get(renderer)==null,"Disconnect did not clear old Azure context");
        Class<?> stack=holderFixture(false);Object empty=stack.getField("field_8037").get(null),normal=stack.getConstructor().newInstance();
        var set=stack.getMethod("method_27320",Entity.class);var holder=stack.getField("field_24093");
        set.invoke(empty,previous);check(holder.get(empty)==null,"Global EMPTY acquired an entity");
        set.invoke(normal,active);check(holder.get(normal)==active,"Normal stack holder blocked");
        set.invoke(normal,new Object[]{null});check(holder.get(normal)==null,"Normal stack holder clear blocked");
        set.invoke(empty,new Object[]{null});check(holder.get(empty)==null,"Native EMPTY clear blocked");
    }
    private static void armorProbes()throws Exception {
        ClassNode full=new ClassNode();new ClassReader(FIXED.get("net.minecraft.class_970")).accept(full,0);
        ClassNode stub=new ClassNode();stub.version=org.objectweb.asm.Opcodes.V17;stub.access=org.objectweb.asm.Opcodes.ACC_PUBLIC;stub.name="net/minecraft/class_970";stub.superName="java/lang/Object";
        for(var field:full.fields)if(field.name.contains("gl_stored")){field.access=org.objectweb.asm.Opcodes.ACC_PUBLIC;stub.fields.add(field);}
        var ctor=new MethodNode(org.objectweb.asm.Opcodes.ACC_PUBLIC,"<init>","()V",null,null);ctor.instructions.add(new org.objectweb.asm.tree.VarInsnNode(org.objectweb.asm.Opcodes.ALOAD,0));ctor.instructions.add(new MethodInsnNode(org.objectweb.asm.Opcodes.INVOKESPECIAL,"java/lang/Object","<init>","()V",false));ctor.instructions.add(new org.objectweb.asm.tree.InsnNode(org.objectweb.asm.Opcodes.RETURN));ctor.maxStack=1;ctor.maxLocals=1;stub.methods.add(ctor);
        String generated="batchverification/runtime/ArmorHolder";
        var remapper=new org.objectweb.asm.commons.Remapper(){public String map(String name){return switch(name){case "net/minecraft/class_970"->generated;case "net/minecraft/class_1309"->"batchverification/VerifyMixinTransforms$Entity";case "net/minecraft/class_1304","net/minecraft/class_1799"->"java/lang/Object";default->name;};}};
        var writer=new org.objectweb.asm.ClassWriter(0);stub.accept(new org.objectweb.asm.commons.ClassRemapper(writer,remapper));
        Class<?> type=new FixtureLoader(Map.of(generated.replace('/','.'),writer.toByteArray())).loadClass(generated.replace('/','.'));Object layer=type.getConstructor().newInstance();
        var owners=Arrays.stream(type.getFields()).filter(f->f.getName().contains("gl_storedEntity")).toList();
        var related=Arrays.stream(type.getFields()).filter(f->f.getName().contains("gl_stored")&&!f.getName().contains("gl_storedEntity")).toList();
        check(owners.size()==2&&related.size()==4,"Native armor context fields lost");
        World current=new ClientWorld(),past=new ClientWorld();Entity active=new Entity(current),old=new Entity(past);
        for(var f:owners)f.set(layer,active);for(var f:related)f.set(layer,new Object());
        check(local.prominence.memory.ArmorWorldContexts.prune(layer,o->((Entity)o).world!=current)==0,"Current armor contexts cleared");
        for(var f:owners)f.set(layer,old);
        check(local.prominence.memory.ArmorWorldContexts.prune(layer,o->((Entity)o).world!=current)==2,"One of the duplicate native owners retained");
        for(var f:owners)check(f.get(layer)==null,"Old armor owner kept");for(var f:related)check(f.get(layer)==null,"Old armor stack/slot kept");
        owners.get(0).set(layer,old);owners.get(1).set(layer,active);for(var f:related)f.set(layer,new Object());
        check(local.prominence.memory.ArmorWorldContexts.prune(layer,o->((Entity)o).world!=current)==1,"Mixed owner cleanup failed");
        for(var f:related)check(f.get(layer)!=null,"Live owner's armor context lost");
        var weak=pruneTemporaryArmor(layer,owners,related);
        for(int i=0;i<30&&weak.get()!=null;i++){System.gc();Thread.sleep(20);}
        check(weak.get()==null,"Retained armor layer/metadata still owns previous world");java.lang.ref.Reference.reachabilityFence(layer);
    }
    private static java.lang.ref.WeakReference<World> pruneTemporaryArmor(Object layer,List<java.lang.reflect.Field> owners,List<java.lang.reflect.Field> related)throws Exception {
        World world=new ClientWorld();Entity entity=new Entity(world);
        for(var field:owners)field.set(layer,entity);for(var field:related)field.set(layer,new Object());
        var weak=new java.lang.ref.WeakReference<>(world);local.prominence.memory.ArmorWorldContexts.prune(layer,o->true);return weak;
    }
    public static void main(String[] args) throws Exception {
        Path configs=Files.createTempDirectory("batch-memory-native-");
        String cfg=Files.readString(Path.of(args[0])).replace("local.prominence.memory.BatchMemoryGuard",OfflinePlugin.class.getName());
        Files.writeString(configs.resolve("offline-batch-memory.mixins.json"),cfg);
        var visual=new com.google.gson.JsonParser().parse(Files.readString(Path.of(args[1]))).getAsJsonObject();
        var nativeCfg=new com.google.gson.JsonObject();nativeCfg.addProperty("required",true);nativeCfg.addProperty("package","com.github.nyuppo.mixin");nativeCfg.addProperty("compatibilityLevel","JAVA_17");
        var mixins=new com.google.gson.JsonArray();for(var source:visual.getAsJsonArray("sources"))mixins.add(source.getAsString().substring(source.getAsString().lastIndexOf('.')+1));
        nativeCfg.add("client",mixins);nativeCfg.addProperty("refmap","moremobvariants-refmap.json");
        var inj=new com.google.gson.JsonObject();inj.addProperty("defaultRequire",1);nativeCfg.add("injectors",inj);
        Files.writeString(configs.resolve("offline-mmv-native.mixins.json"),nativeCfg.toString());
        Files.writeString(configs.resolve("offline-gecko-armor.mixins.json"),"""
          {"required":true,"package":"software.bernie.geckolib.mixins.fabric","compatibilityLevel":"JAVA_17","client":["MixinHumanoidArmorLayer"],"refmap":"geckolib-fabric-1.20.1-refmap.json","injectors":{"defaultRequire":1}}
          """);
        Files.writeString(configs.resolve("offline-azure-armor.mixins.json"),"""
          {"required":true,"package":"mod.azure.azurelibarmor.mixin","compatibilityLevel":"JAVA_17","client":["MixinHumanoidArmorLayer"],"refmap":"azurelibarmor.refmap.json","injectors":{"defaultRequire":1}}
          """);
        List<URL> urls=new ArrayList<>();urls.add(configs.toUri().toURL());for(String path:System.getProperty("java.class.path").split(File.pathSeparator))if(!path.isBlank())urls.add(Path.of(path).toUri().toURL());
        LOADER=new URLClassLoader(urls.toArray(URL[]::new),VerifyMixinTransforms.class.getClassLoader());
        MixinBootstrap.init();Method phase=MixinEnvironment.class.getDeclaredMethod("gotoPhase",MixinEnvironment.Phase.class);phase.setAccessible(true);phase.invoke(null,MixinEnvironment.Phase.INIT);MixinExtrasBootstrap.init();
        MixinEnvironment.getCurrentEnvironment().setObfuscationContext("named:intermediary");
        Mixins.addConfiguration("offline-mmv-native.mixins.json");Mixins.addConfiguration("offline-gecko-armor.mixins.json");Mixins.addConfiguration("offline-azure-armor.mixins.json");Mixins.addConfiguration("offline-batch-memory.mixins.json");
        for(Config c:Mixins.getConfigs()){c.getConfig().decorate(FabricUtil.KEY_MOD_ID,"prominence_korean_compat");c.getConfig().decorate(FabricUtil.KEY_COMPATIBILITY,FabricUtil.COMPATIBILITY_LATEST);}
        phase.invoke(null,MixinEnvironment.Phase.DEFAULT);MixinEnvironment.getCurrentEnvironment().setObfuscationContext("named:intermediary");
        var targets=new ArrayList<String>(List.of("eu.ha3.presencefootsteps.sound.player.DelayedSoundPlayer$PendingSound","eu.ha3.presencefootsteps.sound.player.DelayedSoundPlayer","mod.azure.azurelib.renderer.GeoEntityRenderer","dev.obscuria.aquamirae.client.model.ModelScuttler","net.minecraft.class_1799","net.minecraft.class_970"));
        for(var target:visual.getAsJsonArray("targets"))targets.add(target.getAsString());
        for(String target:targets) {
            byte[] raw;try(var in=LOADER.getResourceAsStream(target.replace('.','/')+".class")){raw=in.readAllBytes();}
            byte[] fixed=TRANSFORMER.transformClassBytes(target,target,raw);if(Arrays.equals(raw,fixed))throw new AssertionError("No transform "+target);
            FIXED.put(target,fixed);Files.write(Path.of(args[2]).resolve(target+".class"),fixed);
            ClassNode original=new ClassNode(),node=new ClassNode();new ClassReader(raw).accept(original,0);new ClassReader(fixed).accept(node,0);
            for(var m:node.methods){new Analyzer<BasicValue>(new BasicVerifier()).analyze(node.name,m);verified++;}
            if(target.equals("mod.azure.azurelib.renderer.GeoEntityRenderer")) {
                for(var old:original.methods)if(!old.name.equals("<init>")) {
                    var same=node.methods.stream().filter(m->m.name.equals(old.name)&&m.desc.equals(old.desc)).findFirst().orElseThrow();
                    if(!calls(old).equals(calls(same)))throw new AssertionError("Azure native render calls changed "+old.name);
                }
                if(!node.interfaces.contains("local/prominence/memory/AzureRendererOwner"))throw new AssertionError("Missing weak-registry owner API");
            }
            if(target.equals("dev.obscuria.aquamirae.client.model.ModelScuttler")) {
                var nativeWalk=original.methods.stream().filter(m->m.name.equals("molangWalkSpeed")).findFirst().orElseThrow();
                var same=node.methods.stream().filter(m->m.name.equals("molangWalkSpeed")).findFirst().orElseThrow();
                if(!calls(nativeWalk).equals(calls(same)))throw new AssertionError("Native walk calculation replaced");
                boolean weakFactory=false;
                for(var m:node.methods)for(var i:m.instructions)if(i instanceof org.objectweb.asm.tree.InvokeDynamicInsnNode dynamic)
                    for(Object a:dynamic.bsmArgs)if(a instanceof org.objectweb.asm.Handle h) {
                        if(h.getOwner().startsWith("local/prominence/memory/batch/"))throw new AssertionError("Unresolved mixin method handle");
                        if(h.getName().equals("molangWalkSpeed")&&h.getOwner().equals("dev/obscuria/aquamirae/client/model/ModelScuttler")) {
                            check(dynamic.desc.equals("()Ljava/util/function/ToDoubleFunction;"),"Walk function captures an entity");weakFactory=true;
                        }
                    }
                check(weakFactory,"Native weak walk function handle absent");
            }
        }
        if(rewrites!=11)throw new AssertionError("Expected 11 native MMV visual projections, got "+rewrites);
        footstepProbes();holderProbes();armorProbes();
        var report=new LinkedHashMap<String,Object>();report.put("status","passed");report.put("targets",targets.size());report.put("methodsBytecodeVerified",verified);report.put("mmvVisualCallsOnly",rewrites);report.put("azureNativeRenderCallsPreserved",true);report.put("aquamiraeNativeWalkPreserved",true);report.put("nativeBehaviorChecks",behaviorChecks);report.put("nativeFootstepBugsReproduced",true);report.put("bothNativeArmorOwnerFields",armorFields);report.put("gameStarted",false);
        Files.writeString(Path.of(args[3]),new GsonBuilder().setPrettyPrinting().create().toJson(report));System.out.println(report);
    }
}
