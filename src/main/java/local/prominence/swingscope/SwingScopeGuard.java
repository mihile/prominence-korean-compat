package local.prominence.swingscope;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.*;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.*;

public final class SwingScopeGuard implements IMixinConfigPlugin {
    private static boolean enabled(){
        try(var in=SwingScopeGuard.class.getResourceAsStream("/prominence-swing-scope-pins.json")){
            var loader=FabricLoader.getInstance();
            if(!loader.getModContainer("minecraft").orElseThrow().getMetadata().getVersion().getFriendlyString().equals("1.20.1")||!loader.getModContainer("mixinextras").orElseThrow().getMetadata().getVersion().getFriendlyString().equals("0.5.4"))return false;
            var pins=JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonObject();
            for(var pin:pins.entrySet()){
                var mod=loader.getModContainer(pin.getKey()).orElseThrow();var expected=pin.getValue().getAsJsonObject();
                if(!mod.getMetadata().getVersion().getFriendlyString().equals(expected.get("version").getAsString()))return false;
                for(var e:expected.getAsJsonObject("classes").entrySet())if(!HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(mod.findPath(e.getKey()).orElseThrow()))).equals(e.getValue().getAsString()))return false;
            }
            return true;
        }catch(Exception unavailable){return false;}
    }
    private boolean active;
    public void onLoad(String pkg){active=enabled();org.slf4j.LoggerFactory.getLogger("Prominence compatibility").info("Weapon animation/attack separation: {}",active?"enabled":"disabled (native version/hash mismatch)");}
    public boolean shouldApplyMixin(String target,String mixin){return active;}
    public String getRefMapperConfig(){return null;} public List<String> getMixins(){return null;} public void acceptTargets(Set<String>a,Set<String>b){} public void preApply(String t,ClassNode n,String m,IMixinInfo i){} public void postApply(String t,ClassNode n,String m,IMixinInfo i){}
}
