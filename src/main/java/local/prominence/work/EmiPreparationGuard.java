package local.prominence.work;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import local.prominence.join.JoinGuard;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public final class EmiPreparationGuard implements IMixinConfigPlugin {
    private final Map<String, Boolean> enabled = new HashMap<>();
    public void onLoad(String pkg) {
        try (var in = getClass().getResourceAsStream("/prominence-emi-preparation-pins.json")) {
            if (in == null) return;
            var pins = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            for (var entry : pins.entrySet()) {
                String group = entry.getKey();
                boolean active = !"false".equalsIgnoreCase(System.getProperty("prominence.emiPreparation." + group, "true"))
                    && JoinGuard.matches(entry.getValue().getAsJsonObject(),
                        id -> FabricLoader.getInstance().getModContainer(id).map(m -> m.getMetadata().getVersion().getFriendlyString()).orElse(null),
                        (id, path) -> { try {
                            // Minecraft's launcher container may not expose its intermediary JAR root.
                            if (id.equals("minecraft")) {
                                try (var stream = getClass().getClassLoader().getResourceAsStream(path)) {
                                    return stream == null ? null : stream.readAllBytes();
                                }
                            }
                            var file = FabricLoader.getInstance().getModContainer(id).orElseThrow().findPath(path);
                            return file.isPresent() ? Files.readAllBytes(file.get()) : null;
                        } catch (Exception | LinkageError e) { return null; } });
                enabled.put(group, active);
                LoggerFactory.getLogger("Prominence compatibility").info("EMI stack preparation: {}={}", group, active ? "enabled" : "disabled");
            }
        } catch (Exception | LinkageError e) { enabled.clear(); }
    }
    public boolean shouldApplyMixin(String target, String mixin) {
        return enabled.getOrDefault(mixin.endsWith("ProminentHiddenItemMixin") ? "hiddenItem" : "anvilPreparation", false);
    }
    public String getRefMapperConfig() { return null; }
    public List<String> getMixins() { return null; }
    public void acceptTargets(Set<String> mine, Set<String> others) {}
    public void preApply(String target, ClassNode node, String name, IMixinInfo info) {}
    public void postApply(String target, ClassNode node, String name, IMixinInfo info) {}
}
