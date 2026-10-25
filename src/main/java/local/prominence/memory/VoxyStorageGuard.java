package local.prominence.memory;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public final class VoxyStorageGuard implements IMixinConfigPlugin {
    private boolean enabled;
    public void onLoad(String pkg){
        try(var in=VoxyStorageGuard.class.getResourceAsStream("/prominence-storage-native-pins.json")){
            var pins=JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonObject();
            enabled=!System.getProperty("prominence.voxyStorageRecovery","true").equalsIgnoreCase("false")
                &&ShaderPreprocessGuard.verifyPins(pins,id->FabricLoader.getInstance().getModContainer(id).map(m->m.getMetadata().getVersion().getFriendlyString()).orElse(null),(id,name)->{
                    try{var p=FabricLoader.getInstance().getModContainer(id).orElseThrow().findPath(name);return p.isPresent()?Files.readAllBytes(p.get()):null;}catch(Exception e){return null;}
                });
        }catch(Exception e){enabled=false;}
        LoggerFactory.getLogger("Prominence compatibility").info("Client Voxy storage lock recovery: {} (client instances only; bounded retries, native locks preserved)",enabled?"enabled":"disabled");
    }
    public boolean shouldApplyMixin(String target,String mixin){return enabled;}
    public String getRefMapperConfig(){return null;}
    public List<String> getMixins(){return null;}
    public void acceptTargets(Set<String> a,Set<String> b){}
    public void preApply(String target,ClassNode node,String name,IMixinInfo info){}
    public void postApply(String target,ClassNode node,String name,IMixinInfo info){}
}
