package local.prominence.work;

import local.prominence.join.JoinGuard;

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

public final class WorkGuard implements IMixinConfigPlugin {
    private final Map<String,Boolean> enabled=new HashMap<>();
    public void onLoad(String pkg) {
        try (var in=WorkGuard.class.getResourceAsStream("/prominence-emi-home-pins.json")) {
            if (in==null) return;
            var pins=JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonObject();
            for (var entry:pins.entrySet()) {
                String name=entry.getKey();
                boolean active=!"false".equalsIgnoreCase(System.getProperty("prominence.emiHome."+name,"true")) && JoinGuard.matches(entry.getValue().getAsJsonObject(),
                    id->FabricLoader.getInstance().getModContainer(id).map(m->m.getMetadata().getVersion().getFriendlyString()).orElse(null),
                    (id,path)->{try {var file=FabricLoader.getInstance().getModContainer(id).orElseThrow().findPath(path);return file.isPresent()?Files.readAllBytes(file.get()):null;}catch(Exception e){return null;}});
                enabled.put(name,active);
                LoggerFactory.getLogger("Prominence compatibility").info("EMI and first-join work: {}={}",name,active?"enabled":"disabled");
            }
        } catch (Exception e) { enabled.clear(); }
    }
    public boolean shouldApplyMixin(String target,String mixin) {
        String group=mixin.endsWith("FacadeCreativeBuildMixin")?"facades":mixin.endsWith("EmiCreativeTimingMixin")?"creativeTiming":mixin.endsWith("LootStableKeyMixin")?"keys":mixin.endsWith("ItemPropertyParsedIdMixin")?"itemIds":mixin.endsWith("EmiInitialWorkMixin")||mixin.endsWith("TerrainInitialBudgetMixin")?"terrain":null;
        return group!=null && enabled.getOrDefault(group,false);
    }
    public String getRefMapperConfig(){return null;}
    public List<String> getMixins(){return null;}
    public void acceptTargets(Set<String> mine,Set<String> others){}
    public void preApply(String target,ClassNode node,String name,IMixinInfo info){}
    public void postApply(String target,ClassNode node,String name,IMixinInfo info){}
}
