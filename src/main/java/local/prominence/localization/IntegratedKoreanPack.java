package local.prominence.localization;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.class_2960;
import org.slf4j.LoggerFactory;
/** Registers the cumulative Korean resource pack shipped inside this JAR. */
public final class IntegratedKoreanPack implements ClientModInitializer {
 public void onInitializeClient(){
  var mod=FabricLoader.getInstance().getModContainer("prominence_korean_compat").orElseThrow();
  boolean registered=ResourceManagerHelper.registerBuiltinResourcePack(new class_2960("prominence_korean_compat","integrated_korean"),mod,"프로미넌스 II 한국어 통합",ResourcePackActivationType.ALWAYS_ENABLED);
  LoggerFactory.getLogger("Prominence compatibility").info("Integrated Korean resources: {}",registered?"registered":"missing");
 }
}
