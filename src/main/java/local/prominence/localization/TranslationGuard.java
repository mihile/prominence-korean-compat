package local.prominence.localization;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.*;

public final class TranslationGuard implements IMixinConfigPlugin {
    private boolean enabled;
    public void onLoad(String pkg){
        try(var pins=TranslationGuard.class.getResourceAsStream("/prominence-translation-native-pins.json");var nativeClass=TranslationGuard.class.getResourceAsStream("/net/minecraft/class_1078.class")){
            var loader=FabricLoader.getInstance();
            var expected=JsonParser.parseReader(new InputStreamReader(pins,StandardCharsets.UTF_8)).getAsJsonObject();
            enabled=!System.getProperty("prominence.translationOverrides","true").equalsIgnoreCase("false")&&loader.getModContainer("minecraft").orElseThrow().getMetadata().getVersion().getFriendlyString().equals("1.20.1")&&loader.getModContainer("mixinextras").orElseThrow().getMetadata().getVersion().getFriendlyString().equals("0.5.4")&&HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(nativeClass.readAllBytes())).equals(expected.get("languageStorageSha256").getAsString());
        }catch(Exception unavailable){enabled=false;}
        org.slf4j.LoggerFactory.getLogger("Prominence compatibility").info("JAR language correction overlay: {}",enabled?"enabled":"disabled (version, fingerprint or opt-out)");
    }
    public boolean shouldApplyMixin(String target,String mixin){return enabled;}
    public String getRefMapperConfig(){return null;}public List<String> getMixins(){return null;}public void acceptTargets(Set<String>a,Set<String>b){}public void preApply(String t,ClassNode n,String m,IMixinInfo i){}public void postApply(String t,ClassNode n,String m,IMixinInfo i){}
}
