package local.prominence.memory;

import com.google.gson.*;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.*;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.*;

public final class ShaderTransformGuard implements IMixinConfigPlugin {
    private boolean enabled;
    public static boolean matches(JsonObject pins, String minecraft, String iris,
            java.util.function.Function<String, byte[]> read) {
        if (!"1.20.1".equals(minecraft) || !"1.7.6+mc1.20.1".equals(iris)) return false;
        try {
            for (var entry : pins.entrySet()) {
                byte[] bytes = read.apply(entry.getKey());
                if (bytes == null || !HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(bytes)).equals(entry.getValue().getAsString())) return false;
            }
            return !pins.isEmpty();
        } catch (Exception failure) { return false; }
    }
    public void onLoad(String pkg) {
        if (!"false".equalsIgnoreCase(System.getProperty("prominence.shaderTransformKey", "true"))) {
            try (var in = ShaderTransformGuard.class.getResourceAsStream("/prominence-transform-key-pins.json")) {
                var loader = FabricLoader.getInstance();
                var iris = loader.getModContainer("iris");
                enabled = in != null && iris.isPresent() && matches(JsonParser.parseReader(
                    new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject(),
                    loader.getModContainer("minecraft").map(m -> m.getMetadata().getVersion().getFriendlyString()).orElse(null),
                    iris.get().getMetadata().getVersion().getFriendlyString(),
                    path -> { try { var file = iris.get().findPath(path); return file.isPresent()
                        ? Files.readAllBytes(file.get()) : null; } catch (Exception failure) { return null; } });
            } catch (Exception failure) { enabled = false; }
        }
        org.slf4j.LoggerFactory.getLogger("Prominence compatibility").info(
            "Iris transform result cache key: {} (native 400-entry cache; isolated parameter snapshot)",
            enabled ? "enabled" : "disabled (native guard)");
    }
    public boolean shouldApplyMixin(String target, String name) { return enabled; }
    public String getRefMapperConfig() { return null; } public List<String> getMixins() { return null; }
    public void acceptTargets(Set<String> mine, Set<String> others) {}
    public void preApply(String target, ClassNode node, String name, IMixinInfo info) {}
    public void postApply(String target, ClassNode node, String name, IMixinInfo info) {}
}
