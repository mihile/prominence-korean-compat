package local.prominence.relicfix;
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
public final class RelicFixGuard implements IMixinConfigPlugin {
 private boolean enabled;
 public void onLoad(String pkg){try{var l=FabricLoader.getInstance();var m=l.getModContainer("prominent").orElseThrow();enabled=m.getMetadata().getVersion().getFriendlyString().equals("4.1.1") && l.getModContainer("minecraft").orElseThrow().getMetadata().getVersion().getFriendlyString().equals("1.20.1") && l.getModContainer("mixinextras").orElseThrow().getMetadata().getVersion().getFriendlyString().equals("0.5.4") && HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(m.findPath("elocindev/prominent/recovery/RelicRecoveryManager.class").orElseThrow()))).equals("fe9595ff88d50b3169c3c5a93c184916b910eefc4476ab33c5fc2c1d22350adc");}catch(Exception e){enabled=false;}LoggerFactory.getLogger("Prominence compatibility").info("Relic ownership name: {}",enabled?"enabled":"disabled");}
 public boolean shouldApplyMixin(String t,String m){return enabled;} public String getRefMapperConfig(){return null;} public List<String> getMixins(){return null;} public void acceptTargets(Set<String>a,Set<String>b){} public void preApply(String t,ClassNode n,String m,IMixinInfo i){} public void postApply(String t,ClassNode n,String m,IMixinInfo i){}
}
