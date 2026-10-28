package local.prominence.lootcredit;
import com.google.gson.JsonParser;import java.nio.charset.StandardCharsets;import java.io.InputStreamReader;import java.nio.file.Files;import java.security.MessageDigest;import java.util.*;import net.fabricmc.loader.api.FabricLoader;import org.objectweb.asm.tree.ClassNode;import org.spongepowered.asm.mixin.extensibility.*;import org.slf4j.LoggerFactory;
public final class LootGuard implements IMixinConfigPlugin {
 private boolean enabled;
 public void onLoad(String pkg){try(var in=getClass().getResourceAsStream("/prominence-loot-credit-pins.json")){
  var loader=FabricLoader.getInstance();if(!loader.getModContainer("minecraft").orElseThrow().getMetadata().getVersion().getFriendlyString().equals("1.20.1")||"false".equals(System.getProperty("prominence.weaponLootCredit","true")))return;
  for(var e:JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonObject().entrySet()){
   var mod=loader.getModContainer(e.getKey()).orElseThrow();var value=e.getValue().getAsJsonObject();if(!mod.getMetadata().getVersion().getFriendlyString().equals(value.get("version").getAsString()))return;
   for(var pin:value.getAsJsonObject("classes").entrySet())if(!HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(mod.findPath(pin.getKey()).orElseThrow()))).equals(pin.getValue().getAsString()))return;
  }enabled=true;
 }catch(Exception e){enabled=false;}finally{LoggerFactory.getLogger("Prominence compatibility").info("Weapon loot credit: {}",enabled?"enabled":"disabled (version/fingerprint/opt-out)");}}
 public boolean shouldApplyMixin(String t,String m){return enabled;}public String getRefMapperConfig(){return null;}public List<String> getMixins(){return null;}public void acceptTargets(Set<String>a,Set<String>b){}public void preApply(String t,ClassNode n,String m,IMixinInfo i){}public void postApply(String t,ClassNode n,String m,IMixinInfo i){}
}
