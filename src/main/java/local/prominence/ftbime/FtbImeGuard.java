package local.prominence.ftbime;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.*;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.*;
public final class FtbImeGuard implements IMixinConfigPlugin {
    private boolean enabled;
    private boolean check(){
        if(!System.getProperty("os.name","").toLowerCase(Locale.ROOT).startsWith("windows")||System.getProperty("prominence.ftbIme","true").equalsIgnoreCase("false"))return false;
        try(var in=FtbImeGuard.class.getResourceAsStream("/prominence-ftb-ime-pins.json")){
            var loader=FabricLoader.getInstance();
            if(!loader.getModContainer("minecraft").orElseThrow().getMetadata().getVersion().getFriendlyString().equals("1.20.1")||!loader.getModContainer("mixinextras").orElseThrow().getMetadata().getVersion().getFriendlyString().equals("0.5.4"))return false;
            var pins=JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonObject();
            for(var pin:pins.entrySet()){
                var mod=loader.getModContainer(pin.getKey()).orElseThrow();var expected=pin.getValue().getAsJsonObject();
                if(!mod.getMetadata().getVersion().getFriendlyString().equals(expected.get("version").getAsString()))return false;
                for(var entry:expected.getAsJsonObject("classes").entrySet())if(!HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(mod.findPath(entry.getKey()).orElseThrow()))).equals(entry.getValue().getAsString()))return false;
            }
            return true;
        }catch(Exception unavailable){return false;}
    }
    public void onLoad(String pkg){enabled=check();org.slf4j.LoggerFactory.getLogger("Prominence compatibility").info("FTB CaramelChat input bridge: {}",enabled?"enabled":"disabled (platform, version, native fingerprint or opt-out)");}
    public boolean shouldApplyMixin(String target,String mixin){return enabled;}
    public String getRefMapperConfig(){return null;}public List<String> getMixins(){return null;}public void acceptTargets(Set<String>a,Set<String>b){}public void preApply(String t,ClassNode n,String m,IMixinInfo i){}public void postApply(String t,ClassNode n,String m,IMixinInfo i){}
}
