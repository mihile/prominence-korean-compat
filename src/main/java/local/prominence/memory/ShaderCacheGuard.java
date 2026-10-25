package local.prominence.memory;

import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/** Only the measured Iris/GLSL implementation is allowed to release its scratch cache. */
public final class ShaderCacheGuard implements IMixinConfigPlugin {
    private boolean enabled;
    private static boolean version(String id, String expected) {
        return FabricLoader.getInstance().getModContainer(id)
            .map(m -> m.getMetadata().getVersion().getFriendlyString().equals(expected)).orElse(false);
    }
    private static boolean irisClass(String name, String hash) {
        try {
            var iris = FabricLoader.getInstance().getModContainer("iris");
            if (iris.isEmpty()) return false;
            var path = iris.get().findPath(name);
            return path.isPresent() && digest(Files.readAllBytes(path.get())).equals(hash);
        } catch (Exception e) { return false; }
    }
    private static boolean dependencyClass(String name, String hash) {
        try (var stream = ShaderCacheGuard.class.getClassLoader().getResourceAsStream(name)) {
            return stream != null && digest(stream.readAllBytes()).equals(hash);
        } catch (Exception e) { return false; }
    }
    private static String digest(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }
    public void onLoad(String mixinPackage) {
        enabled = !System.getProperty("prominence.shaderParseCacheRelease", "true").equalsIgnoreCase("false")
            && version("minecraft", "1.20.1") && version("iris", "1.7.6+mc1.20.1")
            && irisClass("net/irisshaders/iris/pipeline/IrisRenderingPipeline.class", "c62bb0ed3892e7fadeb1e87142cbb35028e69fb411dd33e6151f6052c03290e9")
            && irisClass("net/irisshaders/iris/pipeline/transform/TransformPatcher.class", "93a74acd6636fc8e166e98dfb1adc77b3523c1412d33c8bc0b7672e15830f00d")
            && irisClass("net/irisshaders/iris/pipeline/programs/ShaderMap.class", "639ece4ecd50537f5c5cdb9756c305c6c1d03138de40a8efe6451ab2b661b26e")
            && dependencyClass("io/github/douira/glsl_transformer/ast/transform/ASTParser.class", "219242ef81e466432fa6423027e45d5c05407849f616686ccaae805825c5c264")
            && dependencyClass("io/github/douira/glsl_transformer/parser/CachingParser.class", "31f5d65bb78093448f9a4bb2ceaa1f98525fc9078b67c986752b58da39cd06f0")
            && dependencyClass("io/github/douira/glsl_transformer/ast/data/TypedTreeCache.class", "4b6a76ea983c2aaf5d044e21f3529d92ced11d414f0b36cac66e4e890d0b2fea");
        LoggerFactory.getLogger("Prominence compatibility").info(
            "Client shader parser cache lifetime: {} (exact Iris/GLSL guards; compiled shaders and result cache preserved)",
            enabled ? "enabled" : "disabled");
    }
    public boolean shouldApplyMixin(String target, String mixin) { return enabled; }
    public String getRefMapperConfig() { return null; }
    public List<String> getMixins() { return null; }
    public void acceptTargets(Set<String> mine, Set<String> others) {}
    public void preApply(String target, ClassNode node, String name, IMixinInfo info) {}
    public void postApply(String target, ClassNode node, String name, IMixinInfo info) {}
}
