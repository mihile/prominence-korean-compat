package local.prominence.arrival;

import com.google.gson.*;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.*;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.*;

public final class ArrivalGuard implements IMixinConfigPlugin {
    private boolean enabled;
    public static boolean matches(JsonObject pins,java.util.function.Function<String,String> version,java.util.function.BiFunction<String,String,byte[]> read){
        try{if(!"1.20.1".equals(version.apply("minecraft"))||!"0.5.4".equals(version.apply("mixinextras")))return false;
            for(var entry:pins.entrySet()){
                var mod=entry.getValue().getAsJsonObject();if(!mod.get("version").getAsString().equals(version.apply(entry.getKey())))return false;
                for(var cl:mod.getAsJsonObject("classes").entrySet()){
                    byte[] bytes=read.apply(entry.getKey(),cl.getKey());if(bytes==null||!HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)).equals(cl.getValue().getAsString()))return false;
                }
            }return true;
        }catch(Exception failure){return false;}
    }
    public void onLoad(String pkg){
        try(var in=ArrivalGuard.class.getResourceAsStream("/prominence-arrival-pins.json")){
            if(in==null)return;var pins=JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonObject();
            enabled=!"false".equalsIgnoreCase(System.getProperty("prominence.arrivalProtection","true"))&&matches(pins,
                id->FabricLoader.getInstance().getModContainer(id).map(m->m.getMetadata().getVersion().getFriendlyString()).orElse(null),
                (id,path)->{try{var file=FabricLoader.getInstance().getModContainer(id).orElseThrow().findPath(path);return file.isPresent()?Files.readAllBytes(file.get()):null;}catch(Exception failure){return null;}});
        }catch(Exception failure){enabled=false;}
        org.slf4j.LoggerFactory.getLogger("Prominence compatibility").info("Waystone arrival protection: {}",enabled?"enabled":"disabled (native guard)");
    }
    public boolean shouldApplyMixin(String target,String name){return enabled;}
    public String getRefMapperConfig(){return null;} public List<String> getMixins(){return null;}
    public void acceptTargets(Set<String> mine,Set<String> others){}
    public void preApply(String target,ClassNode node,String name,IMixinInfo info){}
    public void postApply(String target,ClassNode node,String name,IMixinInfo info){}
}
