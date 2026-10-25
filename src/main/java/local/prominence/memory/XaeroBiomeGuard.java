package local.prominence.memory;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.*;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.*;

/** Scope the measured native writer only; other Xaero versions keep their own lifecycle. */
public final class XaeroBiomeGuard implements IMixinConfigPlugin {
    private boolean enabled;
    static boolean matches(String minecraft,String minimap,String extras,Map<String,byte[]> classes){
        if(!"1.20.1".equals(minecraft)||!"26.6.0".equals(minimap)||!"0.5.4".equals(extras))return false;
        try(var in=XaeroBiomeGuard.class.getResourceAsStream("/prominence-xaero-biome-pins.json")){
            var pins=JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonObject();
            for(var pin:pins.entrySet()){
                byte[] bytes=classes.get(pin.getKey());
                if(bytes==null||!HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)).equals(pin.getValue().getAsString()))return false;
            }
            return pins.size()==2;
        }catch(Exception e){return false;}
    }
    public void onLoad(String pkg){
        enabled=false;
        if(!System.getProperty("prominence.xaeroBiomeScope","true").equalsIgnoreCase("false"))try{
            var loader=FabricLoader.getInstance();var mod=loader.getModContainer("xaerominimap").orElseThrow();var bytes=new HashMap<String,byte[]>();
            for(String name:List.of("xaero/common/minimap/write/MinimapWriter.class","xaero/common/minimap/write/biome/BiomeBlendCalculator.class"))bytes.put(name,Files.readAllBytes(mod.findPath(name).orElseThrow()));
            enabled=matches(loader.getModContainer("minecraft").orElseThrow().getMetadata().getVersion().getFriendlyString(),mod.getMetadata().getVersion().getFriendlyString(),loader.getModContainer("mixinextras").orElseThrow().getMetadata().getVersion().getFriendlyString(),bytes);
        }catch(Exception|LinkageError ignored){enabled=false;}
        LoggerFactory.getLogger("Prominence compatibility").info("Client Xaero biome world lifetime: {} (writer-call scope; native blending preserved)",enabled?"enabled":"disabled");
    }
    public boolean shouldApplyMixin(String target,String mixin){return enabled;}
    public String getRefMapperConfig(){return null;}
    public List<String> getMixins(){return null;}
    public void acceptTargets(Set<String> a,Set<String> b){}
    public void preApply(String t,ClassNode n,String name,IMixinInfo i){}
    public void postApply(String t,ClassNode n,String name,IMixinInfo i){}
}
