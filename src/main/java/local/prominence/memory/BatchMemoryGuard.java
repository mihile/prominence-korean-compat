package local.prominence.memory;

import com.google.gson.*;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.*;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.*;

public final class BatchMemoryGuard implements IMixinConfigPlugin {
    private static final JsonObject PINS=readPins();
    private static final Map<String,Boolean> ACTIVE=new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.concurrent.atomic.AtomicInteger VARIANT_TARGETS=new java.util.concurrent.atomic.AtomicInteger();
    private static JsonObject readPins() {
        try(var in=BatchMemoryGuard.class.getClassLoader().getResourceAsStream("assets/prominence_korean_compat/batch-memory-pins.json")) {
            if(in==null)return new JsonObject();return JsonParser.parseString(new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
        }catch(Exception e){return new JsonObject();}
    }
    static JsonObject pins(){return PINS.deepCopy();}
    static boolean matches(String module,Map<String,String> versions,Map<String,byte[]> classes) {
        if(!PINS.has(module))return false;
        try {
            for(JsonElement dependency:PINS.getAsJsonObject(module).getAsJsonArray("dependencies")) {
                JsonObject d=dependency.getAsJsonObject();
                if(!d.get("version").getAsString().equals(versions.get(d.get("mod").getAsString())))return false;
                for(var entry:d.getAsJsonObject("classes").entrySet()) {
                    byte[] bytes=classes.get(entry.getKey());
                    if(bytes==null||!HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)).equals(entry.getValue().getAsString()))return false;
                }
            }
            return true;
        }catch(Exception e){return false;}
    }
    public static boolean active(String module){return ACTIVE.getOrDefault(module,false);}
    public void onLoad(String pkg) {
        for(String module:PINS.keySet()) {
            boolean enabled=!System.getProperty("prominence.memoryBatch."+module,"true").equalsIgnoreCase("false");
            try {
                var loader=FabricLoader.getInstance();
                enabled&=loader.getModContainer("minecraft").map(m->m.getMetadata().getVersion().getFriendlyString().equals("1.20.1")).orElse(false);
                Map<String,String> versions=new HashMap<>();Map<String,byte[]> bytes=new HashMap<>();
                if(enabled)for(JsonElement dependency:PINS.getAsJsonObject(module).getAsJsonArray("dependencies")) {
                    JsonObject d=dependency.getAsJsonObject();String id=d.get("mod").getAsString();var mod=loader.getModContainer(id);
                    if(mod.isEmpty()){enabled=false;break;}
                    versions.put(id,mod.get().getMetadata().getVersion().getFriendlyString());
                    for(String path:d.getAsJsonObject("classes").keySet()) {
                        if(d.has("classLoaderResource")) {
                            try(var in=BatchMemoryGuard.class.getClassLoader().getResourceAsStream(path)){if(in!=null)bytes.put(path,in.readAllBytes());}
                        } else {var file=mod.get().findPath(path);if(file.isPresent())bytes.put(path,Files.readAllBytes(file.get()));}
                    }
                }
                enabled&=matches(module,versions,bytes);
            }catch(Exception|LinkageError e){enabled=false;}
            ACTIVE.put(module,enabled);
        }
        LoggerFactory.getLogger("Prominence compatibility").info("Client batch memory fixes: footsteps={}, azure={}, aquamirae={}, emptyStack={}, variants={}, armor={}",
            status("footsteps"),status("azure"),status("aquamirae"),status("emptyStack"),status("variants"),status("armor"));
    }
    private static String status(String name){return active(name)?"enabled":"disabled";}
    public boolean shouldApplyMixin(String target,String mixin) {
        String simple=mixin.substring(mixin.lastIndexOf('.')+1);
        for(String module:PINS.keySet())for(JsonElement name:PINS.getAsJsonObject(module).getAsJsonArray("mixins"))if(name.getAsString().equals(simple))return active(module);
        return false;
    }
    public String getRefMapperConfig(){return null;}
    public List<String> getMixins(){return null;}
    public void acceptTargets(Set<String> mine,Set<String> others){}
    public void preApply(String target,ClassNode node,String name,IMixinInfo info){}
    public void postApply(String target,ClassNode node,String name,IMixinInfo info) {
        if(name.endsWith("VariantRendererMarkerMixin")) {
            int n=VariantNbtRewrite.rewrite(node);
            if(n!=1)LoggerFactory.getLogger("Prominence compatibility").warn("MMV visual NBT shape differs for {}; preserving native calls",target);
            else if(VARIANT_TARGETS.incrementAndGet()==11)LoggerFactory.getLogger("Prominence compatibility").info("Client MMV visual NBT projection: 11 targets verified");
        }
        if(name.endsWith("ArmorLayerTrackingMixin")) {
            long fields=node.fields.stream().filter(f->f.name.contains("gl_storedEntity")&&f.desc.equals("Lnet/minecraft/class_1309;")).count();
            if(fields==2)LoggerFactory.getLogger("Prominence compatibility").info("Client armor owner fields verified: 2");
            else {ACTIVE.put("armor",false);LoggerFactory.getLogger("Prominence compatibility").warn("Armor owner shape differs; retaining native fields");}
        }
    }
}
