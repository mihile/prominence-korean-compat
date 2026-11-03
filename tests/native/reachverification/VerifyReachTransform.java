package reachverification;

import batchverification.VerifyMixinTransforms;
import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.BasicVerifier;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.FabricUtil;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.transformer.Config;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;

public final class VerifyReachTransform {
    private static byte[] methodBytes(ClassNode node, MethodNode method) {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(node.version, node.access, node.name, null, node.superName, null);
        method.accept(writer); writer.visitEnd(); return writer.toByteArray();
    }
    public static void main(String[] args) throws Exception {
        Path work = Path.of(args[0]), configs = work.resolve("configs"); Files.createDirectories(configs);
        String config;
        try (var in = VerifyReachTransform.class.getResourceAsStream("/prominence-combat-reach.client.mixins.json")) {
            config = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }
        config = config.replace("local.prominence.combatreach.AttackReachGuard", VerifyMixinTransforms.OfflinePlugin.class.getName());
        Files.writeString(configs.resolve("offline-reach.mixins.json"), config);
        var urls = new ArrayList<URL>(); urls.add(configs.toUri().toURL());
        for (String entry : System.getProperty("java.class.path").split(File.pathSeparator))
            if (!entry.isBlank()) urls.add(Path.of(entry).toUri().toURL());
        var loader = new URLClassLoader(urls.toArray(URL[]::new), VerifyReachTransform.class.getClassLoader());
        var field = VerifyMixinTransforms.class.getDeclaredField("LOADER"); field.setAccessible(true); field.set(null, loader);
        MixinBootstrap.init();
        Method phase = MixinEnvironment.class.getDeclaredMethod("gotoPhase", MixinEnvironment.Phase.class); phase.setAccessible(true);
        phase.invoke(null, MixinEnvironment.Phase.INIT); Mixins.addConfiguration("offline-reach.mixins.json");
        for (Config c : Mixins.getConfigs()) {
            c.getConfig().decorate(FabricUtil.KEY_MOD_ID, "prominence_korean_compat");
            c.getConfig().decorate(FabricUtil.KEY_COMPATIBILITY, FabricUtil.COMPATIBILITY_LATEST);
        }
        phase.invoke(null, MixinEnvironment.Phase.DEFAULT);
        field = VerifyMixinTransforms.class.getDeclaredField("TRANSFORMER"); field.setAccessible(true);
        var transformer = (IMixinTransformer) field.get(null);
        String target = "net.bettercombat.client.collision.TargetFinder";
        byte[] raw;
        try (var in = loader.getResourceAsStream(target.replace('.', '/') + ".class")) { raw = in.readAllBytes(); }
        if (!HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw))
                .equals("b3ac5dc6244ecc7e15b61f62d18ae870aa18401b9411045a100affbf853ad0a7")) throw new AssertionError("wrong native target");
        byte[] fixed = transformer.transformClassBytes(target, target, raw);
        if (Arrays.equals(raw, fixed)) throw new AssertionError("Mixin inactive");
        ClassNode before = new ClassNode(), after = new ClassNode();
        new ClassReader(raw).accept(before, 0); new ClassReader(fixed).accept(after, 0);
        int verified = 0, calls = 0;
        for (MethodNode method : after.methods) {
            new Analyzer<BasicValue>(new BasicVerifier()).analyze(after.name, method); verified++;
            for (var instruction : method.instructions) if (instruction instanceof MethodInsnNode call
                    && call.owner.equals("local/prominence/combatreach/AttackReach") && call.name.equals("apply")) calls++;
        }
        if (calls != 1) throw new AssertionError("reach bridge count " + calls);
        var attack = after.methods.stream().filter(m -> m.name.equals("findAttackTargetResult")).findFirst().orElseThrow();
        String first = "";
        for (var instruction : attack.instructions) if (instruction instanceof MethodInsnNode call) { first = call.name; break; }
        if (!first.contains("includeAttackReach")) throw new AssertionError("bonus must precede candidate/geometry work");
        int preserved = 0;
        for (MethodNode method : before.methods) if (!method.name.equals("findAttackTargetResult")) {
            var candidate = after.methods.stream().filter(m -> m.name.equals(method.name) && m.desc.equals(method.desc)).findFirst().orElseThrow();
            if (!Arrays.equals(methodBytes(before, method), methodBytes(after, candidate))) throw new AssertionError("unrelated method changed " + method.name);
            preserved++;
        }
        Files.write(work.resolve("native-target.class"), raw); Files.write(work.resolve("fixed-target.class"), fixed);
        var pins = com.google.gson.JsonParser.parseString(Files.readString(Path.of(args[1]))).getAsJsonObject();
        for (var mod : pins.entrySet()) for (var entry : mod.getValue().getAsJsonObject().getAsJsonObject("classes").entrySet()) {
            byte[] data; try (var in = loader.getResourceAsStream(entry.getKey())) { data = in.readAllBytes(); }
            if (!HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data)).equals(entry.getValue().getAsString())) throw new AssertionError("pin " + entry.getKey());
            Path file = work.resolve("pins").resolve(mod.getKey()).resolve(entry.getKey()); Files.createDirectories(file.getParent()); Files.write(file, data);
        }
        System.out.println("PASS: real native TargetFinder Mixin, " + verified + " ASM methods, " + preserved + " unchanged native methods; bonus before candidate search");
    }
}
