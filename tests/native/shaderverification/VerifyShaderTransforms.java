package shaderverification;

import batchverification.VerifyMixinTransforms;
import com.llamalad7.mixinextras.MixinExtrasBootstrap;
import java.net.*;import java.nio.file.*;import java.io.*;import java.lang.reflect.*;import java.util.*;
import net.irisshaders.iris.config.IrisConfig;
import net.irisshaders.iris.pipeline.transform.*;
import net.irisshaders.iris.pipeline.transform.parameter.*;
import net.irisshaders.iris.shaderpack.texture.TextureStage;
import net.irisshaders.iris.gl.blending.AlphaTest;
import net.irisshaders.iris.gl.state.ShaderAttributeInputs;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.objectweb.asm.*;import org.objectweb.asm.tree.*;import org.objectweb.asm.tree.analysis.*;
import org.spongepowered.asm.launch.MixinBootstrap;import org.spongepowered.asm.mixin.*;import org.spongepowered.asm.mixin.transformer.*;

/** Native Iris transforms and native LRU, without starting Minecraft or opening an OpenGL context. */
public final class VerifyShaderTransforms {
    private static final String PATCHER="net.irisshaders.iris.pipeline.transform.TransformPatcher";
    private static final IrisConfig CONFIG=new IrisConfig(Path.of("unused-offline-iris.properties"));
    public static int calls;
    public static void transformed(){calls++;}
    private static final class NativeLoader extends ClassLoader {
        final Map<String,byte[]> defs;
        NativeLoader(Map<String,byte[]> defs){super(VerifyShaderTransforms.class.getClassLoader());this.defs=defs;}
        protected synchronized Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException {
            Class<?> c=findLoadedClass(name);if(c==null&&defs.containsKey(name)){byte[] b=defs.get(name);c=defineClass(name,b,0,b.length);}
            if(c==null)c=super.loadClass(name,false);if(resolve)resolveClass(c);return c;
        }
    }
    private static byte[] instrument(byte[] raw) {
        var node=new ClassNode();new ClassReader(raw).accept(node,0);
        for(var method:node.methods) {
            if(method.name.equals("transformInternal"))method.instructions.insert(new MethodInsnNode(Opcodes.INVOKESTATIC,"shaderverification/VerifyShaderTransforms","transformed","()V",false));
        }
        var writer=new ClassWriter(0);node.accept(writer);return writer.toByteArray();
    }
    private record Result(List<Object> outputs,int cold,int warm,int cacheSize,long coldNs,long warmNs) {}
    private static Result exercise(Map<String,byte[]> defs) throws Exception {
        var loader=new NativeLoader(defs);Class<?> patcher=loader.loadClass(PATCHER);
        var transform=patcher.getDeclaredMethod("transform",String.class,String.class,String.class,String.class,String.class,String.class,Parameters.class);transform.setAccessible(true);
        var compute=patcher.getDeclaredMethod("transformCompute",String.class,String.class,Parameters.class);compute.setAccessible(true);
        var outputs=new ArrayList<Object>();int cold=0,warm=0;long coldNs=0,warmNs=0;
        for(int round=0;round<3;round++) {
            int before=calls;long start=System.nanoTime();
            for(int variant=0;variant<8;variant++) for(int kind=0;kind<5;kind++) {
                var textures=new Object2ObjectOpenHashMap();var input=new ShaderAttributeInputs(true,true,true,true,true);
                Parameters p=switch(kind){
                    case 0->new VanillaParameters(Patch.VANILLA,textures,AlphaTest.ALWAYS,false,false,input,false,false);
                    case 1->new SodiumParameters(Patch.SODIUM,textures,AlphaTest.ALWAYS,input);
                    case 2->new TextureStageParameters(Patch.COMPOSITE,TextureStage.COMPOSITE_AND_FINAL,textures);
                    case 3->new DHParameters(Patch.DH_GENERIC,textures);
                    default->new ComputeParameters(Patch.COMPUTE,TextureStage.COMPOSITE_AND_FINAL,textures);
                };
                String vs="#version 120\nvoid main(){gl_Position=ftransform();}\n";
                String fs="#version 120\nvoid main(){gl_FragColor=vec4("+variant+".0);}\n";
                Object result=kind==4?compute.invoke(null,"offline_compute","#version 430\nlayout(local_size_x=1) in; void main(){int x="+variant+";}\n",p)
                    :transform.invoke(null,"offline_shader",vs,null,null,null,fs,p);
                outputs.add(new EnumMap((Map)result));
            }
            long elapsed=System.nanoTime()-start;
            if(round==0){cold=calls-before;coldNs=elapsed;}else{warm+=calls-before;warmNs+=elapsed;}
        }
        var cache=patcher.getDeclaredField("cache");cache.setAccessible(true);int size=((Map)cache.get(null)).size();
        return new Result(outputs,cold,warm,size,coldNs,warmNs);
    }
    public static void main(String[] args)throws Exception {
        // Iris's static fields are harmless; supply only an in-memory config. Do not call its
        // initializer, config initialize/load/save, Minecraft client, or GL entry points.
        var irisConfig=Class.forName("net.irisshaders.iris.Iris").getDeclaredField("irisConfig");
        irisConfig.setAccessible(true);irisConfig.set(null,CONFIG);
        Path out=Path.of(args[1]);Files.createDirectories(out);Path cfg=out.resolve("configs");Files.createDirectories(cfg);
        Files.writeString(cfg.resolve("offline-transform-key.mixins.json"),Files.readString(Path.of(args[0])).replace("local.prominence.memory.ShaderTransformGuard",VerifyMixinTransforms.OfflinePlugin.class.getName()));
        var urls=new ArrayList<URL>();urls.add(cfg.toUri().toURL());for(String s:System.getProperty("java.class.path").split(File.pathSeparator))urls.add(Path.of(s).toUri().toURL());
        var loader=new URLClassLoader(urls.toArray(URL[]::new),VerifyShaderTransforms.class.getClassLoader());var f=VerifyMixinTransforms.class.getDeclaredField("LOADER");f.setAccessible(true);f.set(null,loader);
        MixinBootstrap.init();Method phase=MixinEnvironment.class.getDeclaredMethod("gotoPhase",MixinEnvironment.Phase.class);phase.setAccessible(true);phase.invoke(null,MixinEnvironment.Phase.INIT);
        MixinExtrasBootstrap.init();MixinEnvironment.getCurrentEnvironment().setObfuscationContext("named:intermediary");Mixins.addConfiguration("offline-transform-key.mixins.json");
        for(Config c:Mixins.getConfigs()){c.getConfig().decorate(FabricUtil.KEY_MOD_ID,"prominence_korean_compat");c.getConfig().decorate(FabricUtil.KEY_COMPATIBILITY,FabricUtil.COMPATIBILITY_LATEST);}
        phase.invoke(null,MixinEnvironment.Phase.DEFAULT);MixinEnvironment.getCurrentEnvironment().setObfuscationContext("named:intermediary");
        f=VerifyMixinTransforms.class.getDeclaredField("TRANSFORMER");f.setAccessible(true);var transformer=(IMixinTransformer)f.get(null);
        var baseline=new HashMap<String,byte[]>();var candidate=new HashMap<String,byte[]>();int methods=0,hooks=0,constructors=0;
        for(String suffix:List.of("","$CacheKey","$1","$2","$3")) {
            String name=PATCHER+suffix;byte[] raw;try(var in=loader.getResourceAsStream(name.replace('.','/')+".class")){if(in==null)continue;raw=in.readAllBytes();}
            byte[] fixed=raw;
            if(suffix.equals("$CacheKey")) {
                fixed=transformer.transformClassBytes(name,name,raw);if(Arrays.equals(raw,fixed))throw new AssertionError("No key transform");
                var node=new ClassNode();new ClassReader(fixed).accept(node,0);
                for(var m:node.methods){new Analyzer<BasicValue>(new BasicVerifier()).analyze(node.name,m);methods++;
                    for(var i:m.instructions)if(i instanceof MethodInsnNode call){
                        if(call.owner.equals("local/prominence/memory/ShaderTransformKey"))hooks++;
                        if(m.name.equals("<init>")&&call.name.contains("prominence$freezeInputs"))constructors++;
                    }
                }
                Files.write(out.resolve("transformed-key.class"),fixed);
            }
            baseline.put(name,suffix.isEmpty()?instrument(raw):raw);candidate.put(name,suffix.isEmpty()?instrument(fixed):fixed);
        }
        if(hooks!=1||constructors!=2)throw new AssertionError("Both native constructors must snapshot their inputs");
        Result before=exercise(baseline),after=exercise(candidate);
        if(!before.outputs.equals(after.outputs))throw new AssertionError("Actual native GLSL output differs");
        if(before.cold!=40||before.warm!=80||after.cold!=40||after.warm!=0||after.cacheSize!=40)throw new AssertionError("Unexpected native cache behavior: "+before+" / "+after);
        String report="{\"status\":\"passed\",\"nativeMethods\":"+methods+",\"nativeColdTransforms\":"+before.cold+",\"nativeWarmTransforms\":"+before.warm+",\"patchedColdTransforms\":"+after.cold+",\"patchedWarmTransforms\":"+after.warm+",\"outputComparisons\":"+before.outputs.size()+",\"nativeWarmNs\":"+before.warmNs+",\"patchedWarmNs\":"+after.warmNs+",\"gameStarted\":false,\"glContext\":false}";
        Files.writeString(out.resolve("report.json"),report);System.out.println(report);
    }
}
