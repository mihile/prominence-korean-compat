package local.prominence.hammereffect;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Set;
import local.prominence.join.JoinGuard;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public final class HammerEffectGuard implements IMixinConfigPlugin {
    private boolean active;

    public static boolean matchesPins(com.google.gson.JsonObject pins,
                                      java.util.function.Function<String, String> versions,
                                      java.util.function.BiFunction<String, String, byte[]> bytes) {
        return JoinGuard.matches(pins, versions, bytes);
    }

    @Override
    public void onLoad(String mixinPackage) {
        try (var in = HammerEffectGuard.class.getResourceAsStream("/prominence-hammer-effect-pins.json")) {
            if (in == null) {
                active = false;
                return;
            }
            var pins = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            active = !"false".equalsIgnoreCase(System.getProperty("prominence.hammerEffect", "true"))
                && matchesPins(
                    pins,
                    id -> FabricLoader.getInstance().getModContainer(id)
                        .map(m -> m.getMetadata().getVersion().getFriendlyString())
                        .orElse(null),
                    (id, path) -> {
                        try {
                            var file = FabricLoader.getInstance().getModContainer(id).orElseThrow().findPath(path);
                            if (file.isPresent()) {
                                return Files.readAllBytes(file.get());
                            }
                            if ("minecraft".equals(id)) {
                                try (var stream = HammerEffectGuard.class.getClassLoader().getResourceAsStream(path)) {
                                    return stream == null ? null : stream.readAllBytes();
                                }
                            }
                            return null;
                        } catch (Exception | LinkageError e) {
                            return null;
                        }
                    });
        } catch (Exception | LinkageError e) {
            active = false;
        }
        LoggerFactory.getLogger("Prominence compatibility").info(
            "Orbiting effect precision & first-person visibility: {}",
            active ? "enabled" : "disabled");
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return active;
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
