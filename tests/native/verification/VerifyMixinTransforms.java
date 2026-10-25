package verification;

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
import local.prominence.memory.BetterDaysLifecycleRewrite;
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
            appliedMixins.add(mixinClassName);
            if (mixinClassName.endsWith("BetterDaysWorldLifetimeMarkerMixin")) {
                extraSpellPostApplyCalled = true;
                extraSpellPostApplyRewritten = auditRewrite(targetClass);
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


    static int verifiedMethods;
    private static int fallbackProbes(byte[] fixed) {
        for(int scenario=0;scenario<7;scenario++) {
            ClassNode node=new ClassNode();new ClassReader(fixed).accept(node,0);
            List<MethodNode> handlers=node.methods.stream().filter(m->m.name.contains("$betterdaysInjectSetLevel")||m.name.contains("$betterdaysInjectClearLevel")).toList();
            for(var m:handlers)for(var i:m.instructions)if(i instanceof MethodInsnNode call&&call.owner.equals("betterdays/client/TimeInterpolator")&&call.name.equals("onWorldUnload"))call.name="onWorldLoad";
            MethodNode first=handlers.get(0);
            switch(scenario) {
                case 0 -> node.name="some/other/Target";
                case 1 -> node.methods.remove(first);
                case 2 -> first.visibleAnnotations=null;
                case 3 -> first.desc="()V";
                case 4 -> {for(var i:first.instructions)if(i instanceof MethodInsnNode call&&call.owner.equals("betterdays/client/TimeInterpolator"))call.name="unknown";}
                case 5 -> {for(var i:first.instructions)if(i instanceof MethodInsnNode call&&call.owner.equals("betterdays/client/TimeInterpolator"))call.owner="some/other/Interpolator";}
                case 6 -> {for(var i:first.instructions)if(i instanceof MethodInsnNode call&&call.owner.equals("betterdays/client/TimeInterpolator"))call.setOpcode(org.objectweb.asm.Opcodes.INVOKEVIRTUAL);}
            }
            byte[] before=encode(node);
            if(BetterDaysLifecycleRewrite.rewrite(node)!=0||!Arrays.equals(before,encode(node)))throw new AssertionError("Partial or unsafe fallback: "+scenario);
        }
        return 7;
    }
    public static final class World {}
    public static final class WorldWrapper {
        private final World world;
        public WorldWrapper(World world){this.world=world;}
        public static boolean isClientLevel(World world){return world!=null;}
        public Object get(){return world;}
    }
    private static final class FixtureLoader extends ClassLoader {
        FixtureLoader(){super(VerifyMixinTransforms.class.getClassLoader());}
        Class<?> add(String name,byte[] bytes){return defineClass(name,bytes,0,bytes.length);}
    }
    private static int lifecycleProbe(byte[] fixed,boolean baseline) throws Exception {
        ClassNode client=new ClassNode();new ClassReader(fixed).accept(client,0);
        ClassNode holder=new ClassNode();
        try(InputStream in=LOADER.getResourceAsStream("betterdays/client/TimeInterpolator.class")){new ClassReader(in.readAllBytes()).accept(holder,0);}
        // Execute the original native load/unload methods on inert fixture objects only.
        // No Minecraft class is instantiated or initialized by this isolated probe.
        holder.methods.removeIf(m->!Set.of("<init>","onWorldLoad","onWorldUnload").contains(m.name));
        ClassNode handlers=new ClassNode();handlers.version=org.objectweb.asm.Opcodes.V17;handlers.access=org.objectweb.asm.Opcodes.ACC_PUBLIC;
        handlers.name="net/minecraft/class_310";handlers.superName="java/lang/Object";
        handlers.fields.add(new org.objectweb.asm.tree.FieldNode(org.objectweb.asm.Opcodes.ACC_PUBLIC,"field_1687","Lnet/minecraft/class_638;",null,null));
        for(MethodNode m:client.methods) {
            if(!m.name.contains("$betterdaysInjectSetLevel")&&!m.name.contains("$betterdaysInjectClearLevel"))continue;
            m.access=org.objectweb.asm.Opcodes.ACC_PUBLIC;
            m.name=m.name.contains("$betterdaysInjectSetLevel")?"setWorld":"clearWorld";
            if(baseline)for(var i:m.instructions)if(i instanceof MethodInsnNode call&&call.owner.equals("betterdays/client/TimeInterpolator")&&call.name.equals("onWorldUnload"))call.name="onWorldLoad";
            handlers.methods.add(m);
        }
        var ctor=new MethodNode(org.objectweb.asm.Opcodes.ACC_PUBLIC,"<init>","()V",null,null);
        ctor.instructions.add(new org.objectweb.asm.tree.VarInsnNode(org.objectweb.asm.Opcodes.ALOAD,0));
        ctor.instructions.add(new MethodInsnNode(org.objectweb.asm.Opcodes.INVOKESPECIAL,"java/lang/Object","<init>","()V",false));
        ctor.instructions.add(new org.objectweb.asm.tree.InsnNode(org.objectweb.asm.Opcodes.RETURN));ctor.maxStack=1;ctor.maxLocals=1;handlers.methods.add(ctor);
        String prefix="verification/runtime/"+(baseline?"Baseline":"Fixed");
        var remap=new org.objectweb.asm.commons.Remapper(){
            public String map(String name){return switch(name){
                case "net/minecraft/class_310" -> prefix+"Client";
                case "net/minecraft/class_638","net/minecraft/class_1936" -> "verification/VerifyMixinTransforms$World";
                case "betterdays/client/TimeInterpolator" -> prefix+"Interpolator";
                case "betterdays/wrappers/ClientLevelWrapper" -> "verification/VerifyMixinTransforms$WorldWrapper";
                default -> name;
            };}
        };
        FixtureLoader loader=new FixtureLoader();
        var hw=new org.objectweb.asm.ClassWriter(0);holder.accept(new org.objectweb.asm.commons.ClassRemapper(hw,remap));
        Class<?> type=loader.add((prefix+"Interpolator").replace('/','.'),hw.toByteArray());
        var cw=new org.objectweb.asm.ClassWriter(0);handlers.accept(new org.objectweb.asm.commons.ClassRemapper(cw,remap));
        Class<?> ct=loader.add((prefix+"Client").replace('/','.'),cw.toByteArray());
        Object c=ct.getConstructor().newInstance();var worldField=ct.getField("field_1687");
        var instance=type.getField("instance");var level=type.getField("level");
        var load=type.getMethod("onWorldLoad",World.class);var set=ct.getMethod("setWorld",World.class,org.spongepowered.asm.mixin.injection.callback.CallbackInfo.class);
        var clear=ct.getMethod("clearWorld",org.spongepowered.asm.mixin.injection.callback.CallbackInfo.class);
        World first=new World(),next=new World();
        load.invoke(null,first);set.invoke(c,first,null);
        if(((WorldWrapper)level.get(instance.get(null))).get()!=first)throw new AssertionError("First join registration lost");
        worldField.set(c,first);load.invoke(null,next); // native ClientWorld constructor precedes setWorld
        set.invoke(c,next,null);
        Object selected=((WorldWrapper)level.get(instance.get(null))).get();
        if(selected!=(baseline?first:next))throw new AssertionError("World-switch probe failed");
        worldField.set(c,next);clear.invoke(c,new Object[]{null});
        if((instance.get(null)==null)==baseline)throw new AssertionError("Disconnect probe failed");
        // Delayed unload of the old world cannot erase an already registered new world.
        if(!baseline){load.invoke(null,next);worldField.set(c,first);clear.invoke(c,new Object[]{null});
            if(((WorldWrapper)level.get(instance.get(null))).get()!=next)throw new AssertionError("Old unload erased new interpolation");}
        return baseline?3:4;
    }
    private static byte[] encode(ClassNode node) {
        var writer=new org.objectweb.asm.ClassWriter(0);node.accept(writer);return writer.toByteArray();
    }
    private static boolean auditRewrite(ClassNode target) {
        byte[] before=encode(target);
        if(BetterDaysLifecycleRewrite.rewrite(target)!=2)throw new AssertionError("Native handlers not rewritten");
        List<MethodInsnNode> changed=new ArrayList<>();
        for(MethodNode m:target.methods)for(AbstractInsnNode i:m.instructions)
            if(i instanceof MethodInsnNode call && call.owner.equals("betterdays/client/TimeInterpolator")&&call.name.equals("onWorldUnload"))changed.add(call);
        if(changed.size()!=2)throw new AssertionError("Not exactly two native calls");
        for(var call:changed)call.name="onWorldLoad";
        if(!Arrays.equals(before,encode(target)))throw new AssertionError("Unexpected body change");
        for(var call:changed)call.name="onWorldUnload";
        if(BetterDaysLifecycleRewrite.rewrite(target)!=0)throw new AssertionError("Repeated rewrite changed code");
        for(MethodNode m:target.methods) {
            try{new Analyzer<BasicValue>(new BasicVerifier()).analyze(target.name,m);verifiedMethods++;}
            catch(Exception e){throw new AssertionError(m.name+m.desc,e);}
        }
        return true;
    }
    public static void main(String[] args) throws Exception {
        Path configs=Files.createTempDirectory("betterdays-lifetime-offline-");
        String cfg=Files.readString(Path.of(args[0])).replace("local.prominence.memory.BetterDaysLifecycleGuard",OfflinePlugin.class.getName());
        Files.writeString(configs.resolve("offline-betterdays-fix.mixins.json"),cfg);
        Files.writeString(configs.resolve("offline-betterdays-native.mixins.json"),"""
          {"required":true,"package":"betterdays.mixin","compatibilityLevel":"JAVA_17",
           "client":["MinecraftClientMixin","ClientLevelMixin"],"refmap":"betterdays-refmap.json",
           "injectors":{"defaultRequire":1}}
          """);
        List<URL> urls=new ArrayList<>();urls.add(configs.toUri().toURL());
        for(String path:System.getProperty("java.class.path").split(File.pathSeparator))if(!path.isBlank())urls.add(Path.of(path).toUri().toURL());
        LOADER=new URLClassLoader(urls.toArray(URL[]::new),VerifyMixinTransforms.class.getClassLoader());
        MixinBootstrap.init();Method phase=MixinEnvironment.class.getDeclaredMethod("gotoPhase",MixinEnvironment.Phase.class);
        phase.setAccessible(true);phase.invoke(null,MixinEnvironment.Phase.INIT);MixinExtrasBootstrap.init();
        MixinEnvironment.getCurrentEnvironment().setObfuscationContext("named:intermediary");
        Mixins.addConfiguration("offline-betterdays-native.mixins.json");
        Mixins.addConfiguration("offline-betterdays-fix.mixins.json");
        for(Config c:Mixins.getConfigs()) {
            c.getConfig().decorate(FabricUtil.KEY_MOD_ID,"prominence_korean_compat");
            c.getConfig().decorate(FabricUtil.KEY_COMPATIBILITY,FabricUtil.COMPATIBILITY_LATEST);
        }
        phase.invoke(null,MixinEnvironment.Phase.DEFAULT);
        MixinEnvironment.getCurrentEnvironment().setObfuscationContext("named:intermediary");
        int probes=0,fallbacks=0;
        for(String target:List.of("net.minecraft.class_310","net.minecraft.class_638")) {
            byte[] raw;try(InputStream in=LOADER.getResourceAsStream(target.replace('.','/')+".class")){raw=in.readAllBytes();}
            byte[] transformed=TRANSFORMER.transformClassBytes(target,target,raw);
            if(Arrays.equals(raw,transformed))throw new AssertionError("No native transformation: "+target);
            Files.write(Path.of(args[2]).resolve(target+".class"),transformed);
            if(target.endsWith("class_310")){probes+=lifecycleProbe(transformed,true);probes+=lifecycleProbe(transformed,false);fallbacks=fallbackProbes(transformed);}
            ClassNode node=new ClassNode();new ClassReader(transformed).accept(node,0);
            if(target.endsWith("class_638")) {
                int load=0,unload=0;
                for(MethodNode m:node.methods)for(var i:m.instructions)if(i instanceof MethodInsnNode call&&call.owner.equals("betterdays/client/TimeInterpolator")) {
                    if(call.name.equals("onWorldLoad"))load++;if(call.name.equals("onWorldUnload"))unload++;
                }
                if(load!=1||unload!=0)throw new AssertionError("New world constructor registration changed");
            }
        }
        if(!OfflinePlugin.extraSpellPostApplyCalled||!OfflinePlugin.extraSpellPostApplyRewritten)throw new AssertionError("Marker postApply did not run");
        Map<String,Object> result=new LinkedHashMap<>();result.put("status","passed");result.put("appliedMixins",OfflinePlugin.appliedMixins);
        result.put("exactlyTwoNativeCallsChanged",true);result.put("allOtherNativeBytecodePreserved",true);
        result.put("newWorldConstructorRegistrationPreserved",true);result.put("nativeLifecycleProbes",probes);result.put("nativeBaselineReproduced",true);
        result.put("atomicFallbackProbes",fallbacks);
        result.put("methodsBytecodeVerified",verifiedMethods);result.put("gameStarted",false);
        Files.writeString(Path.of(args[1]),new GsonBuilder().setPrettyPrinting().create().toJson(result));System.out.println(result);
    }
}
