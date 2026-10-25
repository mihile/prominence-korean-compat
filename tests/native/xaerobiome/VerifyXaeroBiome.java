package xaerobiome;
import batchverification.VerifyMixinTransforms;
import com.llamalad7.mixinextras.MixinExtrasBootstrap;
import java.io.*;import java.net.*;import java.nio.file.*;import java.lang.reflect.*;import java.util.*;
import org.objectweb.asm.*;import org.objectweb.asm.tree.*;import org.objectweb.asm.tree.analysis.*;
import org.spongepowered.asm.launch.MixinBootstrap;import org.spongepowered.asm.mixin.*;import org.spongepowered.asm.mixin.transformer.*;
public final class VerifyXaeroBiome {
    static List<String> calls(MethodNode m){var out=new ArrayList<String>();for(var i:m.instructions)if(i instanceof MethodInsnNode c)out.add(c.owner+"."+c.name+c.desc);return out;}
    public static void main(String[]args)throws Exception {
        Path root=Path.of(args[0]),out=Path.of(args[1]),configs=out.resolve("configs");Files.createDirectories(configs);
        String config=Files.readString(root.resolve("prominence-xaero-biome.client.mixins.json")).replace("local.prominence.memory.XaeroBiomeGuard",VerifyMixinTransforms.OfflinePlugin.class.getName());Files.writeString(configs.resolve("offline-xaero.json"),config);
        var urls=new ArrayList<URL>();urls.add(configs.toUri().toURL());for(String p:System.getProperty("java.class.path").split(File.pathSeparator))if(!p.isBlank())urls.add(Path.of(p).toUri().toURL());
        var loader=new URLClassLoader(urls.toArray(URL[]::new),VerifyXaeroBiome.class.getClassLoader());var f=VerifyMixinTransforms.class.getDeclaredField("LOADER");f.setAccessible(true);f.set(null,loader);
        MixinBootstrap.init();Method phase=MixinEnvironment.class.getDeclaredMethod("gotoPhase",MixinEnvironment.Phase.class);phase.setAccessible(true);phase.invoke(null,MixinEnvironment.Phase.INIT);MixinExtrasBootstrap.init();
        MixinEnvironment.getCurrentEnvironment().setSide(MixinEnvironment.Side.CLIENT);MixinEnvironment.getCurrentEnvironment().setObfuscationContext("named:intermediary");Mixins.addConfiguration("offline-xaero.json");
        for(Config c:Mixins.getConfigs()){c.getConfig().decorate(FabricUtil.KEY_MOD_ID,"prominence_korean_compat");c.getConfig().decorate(FabricUtil.KEY_COMPATIBILITY,FabricUtil.COMPATIBILITY_LATEST);}
        phase.invoke(null,MixinEnvironment.Phase.DEFAULT);f=VerifyMixinTransforms.class.getDeclaredField("TRANSFORMER");f.setAccessible(true);var transformer=(IMixinTransformer)f.get(null);
        int methods=0,preserved=0;
        for(String type:List.of("xaero.common.minimap.write.MinimapWriter","xaero.common.minimap.write.biome.BiomeBlendCalculator")){
            byte[] raw;try(var in=loader.getResourceAsStream(type.replace('.','/')+".class")){raw=in.readAllBytes();}
            byte[] fixed=transformer.transformClassBytes(type,type,raw);if(Arrays.equals(raw,fixed))throw new AssertionError("Not transformed "+type);
            var before=new ClassNode();var after=new ClassNode();new ClassReader(raw).accept(before,0);new ClassReader(fixed).accept(after,0);
            for(var m:after.methods){new Analyzer<BasicValue>(new BasicVerifier()).analyze(after.name,m);methods++;}
            for(var old:before.methods){
                var same=after.methods.stream().filter(m->m.desc.equals(old.desc)&&calls(m).equals(calls(old))).findFirst();
                if(same.isEmpty())throw new AssertionError("Native method call sequence lost: "+old.name+old.desc);preserved++;
            }
            if(type.endsWith("MinimapWriter")){
                var nativeBody=after.methods.stream().filter(m->m.name.startsWith("writeChunk$")).findFirst().orElseThrow();
                long prepares=calls(nativeBody).stream().filter(c->c.contains("BiomeBlendCalculator.prepare")).count();if(prepares!=1)throw new AssertionError("Native prepare missing/duplicated");
                var wrapped=after.methods.stream().filter(m->m.name.equals("writeChunk")).findFirst().orElseThrow();
                if(calls(wrapped).stream().noneMatch(c->c.contains("prominence$worldDuringWrite")))throw new AssertionError("No lifetime wrapper");
            }
            Path target=out.resolve("transformed/"+type.replace('.','/')+".class");Files.createDirectories(target.getParent());Files.write(target,fixed);
        }
        Files.writeString(out.resolve("result.json"),"{\"targets\":2,\"methods\":"+methods+",\"nativeMethodCallSequencesPreserved\":"+preserved+",\"gameStarted\":false}");
        System.out.println("PASS: 2 actual Xaero targets, "+methods+" methods; "+preserved+" native call sequences preserved, prepare/blending/bounds unchanged.");
    }
}
