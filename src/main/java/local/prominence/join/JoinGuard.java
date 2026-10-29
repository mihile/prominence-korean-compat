package local.prominence.join;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public final class JoinGuard implements IMixinConfigPlugin {
    private final Map<String,Boolean> enabled=new HashMap<>();
    public static boolean matches(JsonObject group, Function<String,String> versions, BiFunction<String,String,byte[]> classes) {
        try {
            if (!"1.20.1".equals(versions.apply("minecraft")) || !"0.5.4".equals(versions.apply("mixinextras"))) return false;
            for (var entry:group.entrySet()) {
                var mod=entry.getValue().getAsJsonObject();
                if (!mod.get("version").getAsString().equals(versions.apply(entry.getKey()))) return false;
                for (var pin:mod.getAsJsonObject("classes").entrySet()) {
                    byte[] bytes=classes.apply(entry.getKey(),pin.getKey());
                    if (bytes==null || !HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)).equals(pin.getValue().getAsString())) return false;
                }
            }
            return true;
        } catch (Exception e) { return false; }
    }
    public void onLoad(String pkg) {
        try (var in=JoinGuard.class.getResourceAsStream("/prominence-join-pins.json")) {
            if (in==null) return;
            var pins=JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonObject();
            for (var entry:pins.entrySet()) {
                String name=entry.getKey();
                boolean active=!"false".equalsIgnoreCase(System.getProperty("prominence.join."+name,"true")) && matches(entry.getValue().getAsJsonObject(),
                    id->FabricLoader.getInstance().getModContainer(id).map(m->m.getMetadata().getVersion().getFriendlyString()).orElse(null),
                    (id,path)->{try {var file=FabricLoader.getInstance().getModContainer(id).orElseThrow().findPath(path);return file.isPresent()?Files.readAllBytes(file.get()):null;}catch(Exception e){return null;}});
                if (name.equals("menu") && JoinGuard.class.getClassLoader().getResource("com/cinemamod/mcef/MCEF.class")!=null) active=false;
                enabled.put(name,active);
                LoggerFactory.getLogger("Prominence compatibility").info("Client join work reduction: {}={}",name,active?"enabled":"disabled");
            }
        } catch (Exception e) { enabled.clear(); }
    }
    public boolean shouldApplyMixin(String target,String mixin) {
        String group=mixin.endsWith("IdleMusicMixin")?"music":mixin.endsWith("MenuBrowserPresenceMixin")?"menu":mixin.endsWith("IrisMaterialCapacityMixin")?"material":mixin.endsWith("IrisMaterialPredicateMixin")?"materialPredicates":null;
        return group!=null && enabled.getOrDefault(group,false);
    }
    public String getRefMapperConfig(){return null;}
    public List<String> getMixins(){return null;}
    public void acceptTargets(Set<String> mine,Set<String> others){}
    public void preApply(String target,ClassNode node,String name,IMixinInfo info){}
    public void postApply(String target,ClassNode node,String name,IMixinInfo info){}
}
