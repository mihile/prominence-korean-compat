package local.prominence.backpackinline;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
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

public final class InlineGuard implements IMixinConfigPlugin {
    private static final class Check {static final boolean ENABLED=check();}
    private static boolean check(){
        try(var in=InlineGuard.class.getResourceAsStream("/prominence-inline-reforge-native-pins.json")){
            if(System.getProperty("prominence.backpackInlineReforge","true").equalsIgnoreCase("false"))return false;
            var loader=FabricLoader.getInstance();
            if(!loader.getModContainer("minecraft").orElseThrow().getMetadata().getVersion().getFriendlyString().equals("1.20.1"))return false;
            if(!loader.getModContainer("mixinextras").orElseThrow().getMetadata().getVersion().getFriendlyString().equals("0.5.4"))return false;
            var pins=JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonObject();
            for(var pin:pins.entrySet()){
                var mod=loader.getModContainer(pin.getKey()).orElseThrow();var expected=pin.getValue().getAsJsonObject();
                if(!mod.getMetadata().getVersion().getFriendlyString().equals(expected.get("version").getAsString()))return false;
                for(var entry:expected.getAsJsonObject("classes").entrySet()){
                    var path=mod.findPath(entry.getKey()).orElseThrow();
                    if(!HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path))).equals(entry.getValue().getAsString()))return false;
                }
            }
            return true;
        }catch(Exception e){return false;}
    }
    public static boolean enabled(){return Check.ENABLED;}
    public void onLoad(String pkg){LoggerFactory.getLogger("Prominence compatibility").info("Backpack inline reforge: {} (native delegate; negotiated slot layout)",enabled()?"enabled":"disabled");}
    public boolean shouldApplyMixin(String target,String mixin){return enabled();}
    public String getRefMapperConfig(){return null;}
    public List<String> getMixins(){return null;}
    public void acceptTargets(Set<String> a,Set<String> b){}
    public void preApply(String t,ClassNode n,String name,IMixinInfo i){}
    public void postApply(String t,ClassNode n,String name,IMixinInfo i){}
}
