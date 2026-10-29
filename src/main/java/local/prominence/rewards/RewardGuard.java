package local.prominence.rewards;
import com.google.gson.*;import java.io.*;import java.nio.charset.StandardCharsets;import java.security.MessageDigest;import java.util.*;import net.fabricmc.loader.api.FabricLoader;import org.objectweb.asm.tree.ClassNode;import org.spongepowered.asm.mixin.extensibility.*;
public final class RewardGuard implements IMixinConfigPlugin {
 private boolean enabled;
 public void onLoad(String pkg){try(var in=getClass().getResourceAsStream("/prominence-reward-scaling-pins.json")){
  if("false".equals(System.getProperty("prominence.rewardScaling","true")))return;
  var loader=FabricLoader.getInstance();var root=JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonObject();
  for(var mod:root.entrySet()){
   var container=loader.getModContainer(mod.getKey()).orElseThrow();var value=mod.getValue().getAsJsonObject();if(!value.get("version").getAsString().equals(container.getMetadata().getVersion().getFriendlyString()))return;
   for(var entry:value.getAsJsonObject("classes").entrySet()){
    byte[] bytes;try(var input=getClass().getClassLoader().getResourceAsStream(entry.getKey())){if(input==null)return;bytes=input.readAllBytes();}
    String sha=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));boolean found=false;
    for(var allowed:entry.getValue().getAsJsonArray())if(sha.equals(allowed.getAsString()))found=true;if(!found)return;
   }
  }enabled=true;
 }catch(Exception e){enabled=false;}finally{org.slf4j.LoggerFactory.getLogger("Prominence compatibility").info("Rare reward scaling: {}",enabled?"enabled (server data; native existing bonuses preserved)":"disabled (version/fingerprint/opt-out)");}}
 public boolean shouldApplyMixin(String t,String m){return enabled;}public String getRefMapperConfig(){return null;}public List<String> getMixins(){return null;}public void acceptTargets(Set<String>a,Set<String>b){}public void preApply(String t,ClassNode n,String m,IMixinInfo i){}public void postApply(String t,ClassNode n,String m,IMixinInfo i){}
}
