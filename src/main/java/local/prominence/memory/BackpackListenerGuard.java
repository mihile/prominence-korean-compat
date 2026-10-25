package local.prominence.memory;

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

public final class BackpackListenerGuard implements IMixinConfigPlugin {
    private boolean enabled;
    private static String sha(byte[] raw) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw));
    }
    private static boolean dependency(String path, String expected) {
        try (var in=BackpackListenerGuard.class.getClassLoader().getResourceAsStream(path)) {
            return in!=null && sha(in.readAllBytes()).equals(expected);
        } catch(Exception e) { return false; }
    }
    public void onLoad(String pkg) {
        try {
            var loader=FabricLoader.getInstance();
            var mod=loader.getModContainer("sophisticatedbackpacks");
            var path=mod.isPresent()?mod.get().findPath("net/p3pp3rf1y/sophisticatedbackpacks/backpack/BackpackBlockEntity.class"):java.util.Optional.<java.nio.file.Path>empty();
            enabled=!System.getProperty("prominence.backpackWeakListeners","true").equalsIgnoreCase("false")
                && loader.getModContainer("minecraft").map(m->m.getMetadata().getVersion().getFriendlyString().equals("1.20.1")).orElse(false)
                && mod.isPresent() && mod.get().getMetadata().getVersion().getFriendlyString().equals("1.20.1-3.23.4.5.110")
                && path.isPresent() && sha(Files.readAllBytes(path.get())).equals("d1649927698d7ffe138f1979f4b4eab8db437df45910b99819492c8c59746ec7")
                && dependency("net/fabricmc/fabric/api/event/lifecycle/v1/ServerChunkEvents.class","add011281682221a75075b4d1ff45ae280e84e77ac83c705fff0f071b5c73e85")
                && dependency("net/fabricmc/fabric/api/event/lifecycle/v1/ServerBlockEntityEvents.class","7b60229f09ce98c4414d2fb70b0a630bf926e7cb542622be7850ee25242de56d");
        } catch(Exception e) { enabled=false; }
        LoggerFactory.getLogger("Prominence compatibility").info(
            "Client backpack unload listener lifetime: {} (exact target guards; native callbacks preserved)",enabled?"enabled":"disabled");
    }
    public boolean shouldApplyMixin(String target,String mixin) { return enabled; }
    public String getRefMapperConfig(){return null;}
    public List<String> getMixins(){return null;}
    public void acceptTargets(Set<String> mine,Set<String> others){}
    public void preApply(String target,ClassNode node,String name,IMixinInfo info){}
    public void postApply(String target,ClassNode node,String name,IMixinInfo info){}
}
