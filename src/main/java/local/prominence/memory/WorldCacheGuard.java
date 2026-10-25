package local.prominence.memory;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.*;
import net.fabricmc.loader.api.FabricLoader;

/** Each optional cleanup is independently pinned; no dependency version is widened. */
final class WorldCacheGuard {
    record Pin(String mod,String version,Map<String,String> classes) {}
    static List<Pin> pins(String module) {
        try(var in=WorldCacheGuard.class.getResourceAsStream("/prominence-world-cache-pins.json")) {
            var root=JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonObject();
            if(!root.has(module))return List.of();
            var result=new ArrayList<Pin>();
            for(var element:root.getAsJsonArray(module)) {
                var p=element.getAsJsonObject();var classes=new LinkedHashMap<String,String>();
                for(var e:p.getAsJsonObject("classes").entrySet())classes.put(e.getKey(),e.getValue().getAsString());
                result.add(new Pin(p.get("mod").getAsString(),p.get("version").getAsString(),Map.copyOf(classes)));
            }
            return List.copyOf(result);
        }catch(Exception e){return List.of();}
    }
    static boolean matches(String module,Map<String,String> versions,Map<String,byte[]> bytes) {
        var pins=pins(module);if(pins.isEmpty())return false;
        try {
            for(var pin:pins) {
                if(!pin.version().equals(versions.get(pin.mod())))return false;
                for(var p:pin.classes().entrySet()) {
                    byte[] raw=bytes.get(pin.mod()+"/"+p.getKey());
                    if(raw==null||!HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw)).equals(p.getValue()))return false;
                }
            }
            return true;
        }catch(Exception e){return false;}
    }
    static boolean installed(String module) {
        if(System.getProperty("prominence.worldCacheCleanup","true").equalsIgnoreCase("false")
            ||System.getProperty("prominence.worldCacheCleanup."+module,"true").equalsIgnoreCase("false"))return false;
        try {
            var loader=FabricLoader.getInstance();
            if(!loader.getModContainer("minecraft").orElseThrow().getMetadata().getVersion().getFriendlyString().equals("1.20.1"))return false;
            var versions=new HashMap<String,String>();var bytes=new HashMap<String,byte[]>();
            for(var pin:pins(module)) {
                var mod=loader.getModContainer(pin.mod()).orElseThrow();versions.put(pin.mod(),mod.getMetadata().getVersion().getFriendlyString());
                for(String path:pin.classes().keySet())bytes.put(pin.mod()+"/"+path,Files.readAllBytes(mod.findPath(path).orElseThrow()));
            }
            return matches(module,versions,bytes);
        }catch(Exception|LinkageError e){return false;}
    }
}
