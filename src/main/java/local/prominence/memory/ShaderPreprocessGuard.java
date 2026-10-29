package local.prominence.memory;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/** Fail closed on a changed Iris/JCPP implementation; all pins describe external classes. */
public final class ShaderPreprocessGuard implements IMixinConfigPlugin {
    private boolean enabled;
    static JsonObject pins() {
        try(var in=ShaderPreprocessGuard.class.getResourceAsStream("/prominence-preprocess-native-pins.json")) {
            if(in==null)return null;
            return JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonObject();
        }catch(Exception e){return null;}
    }
    static String cacheNamespace() {
        var p=pins();return HexFormat.of().formatHex(ShaderSourceDiskCache.hash(
            ("shader-preprocess-v1:"+(p==null?"unavailable":p.toString())).getBytes(StandardCharsets.UTF_8)));
    }
    static boolean verifyPins(JsonObject pins,Function<String,String> versions,BiFunction<String,String,byte[]> classes) {
        try {
            if(pins==null || !"1.20.1".equals(versions.apply("minecraft")))return false;
            for(var entry:pins.entrySet()) {
                var mod=entry.getValue().getAsJsonObject();
                if(!mod.get("version").getAsString().equals(versions.apply(entry.getKey())))return false;
                for(var pin:mod.getAsJsonObject("classes").entrySet()) {
                    byte[] bytes=classes.apply(entry.getKey(),pin.getKey());
                    if(bytes==null||!HexFormat.of().formatHex(ShaderSourceDiskCache.hash(bytes)).equals(pin.getValue().getAsString()))return false;
                }
            }
            return true;
        }catch(Exception e){return false;}
    }
    public void onLoad(String pkg) {
        enabled=!System.getProperty("prominence.pipelineTiming","true").equalsIgnoreCase("false")
            && verifyPins(pins(),id->FabricLoader.getInstance().getModContainer(id).map(m->m.getMetadata().getVersion().getFriendlyString()).orElse(null),(id,name)->{
                try {var p=FabricLoader.getInstance().getModContainer(id).orElseThrow().findPath(name);return p.isPresent()?Files.readAllBytes(p.get()):null;}
                catch(Exception e){return null;}
            });
        LoggerFactory.getLogger("Prominence compatibility").info("Client shader source disk cache: retired (native preprocessing; pipeline timing={})",enabled?"enabled":"disabled");
    }
    public boolean shouldApplyMixin(String target,String mixin){return enabled && mixin.endsWith("IrisPipelineTimingMixin");}
    public String getRefMapperConfig(){return null;}
    public List<String> getMixins(){return null;}
    public void acceptTargets(Set<String> mine,Set<String> others){}
    public void preApply(String target,ClassNode node,String name,IMixinInfo info){}
    public void postApply(String target,ClassNode node,String name,IMixinInfo info){}
}
