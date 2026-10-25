package local.prominence.memory;

import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public final class BetterDaysLifecycleGuard implements IMixinConfigPlugin {
    private boolean enabled;
    private static final Map<String,String> PINNED=Map.of(
        "betterdays/client/TimeInterpolator.class","935536d1b1971288a3e28abde98da1c82b1de83e1fc4d8112115c3bfe8ba3a37",
        "betterdays/mixin/MinecraftClientMixin.class","38c85681dc0e895e88bb6befc6254d360073ddd2e5f7facc40c076a9444319d1",
        "betterdays/mixin/ClientLevelMixin.class","6728f4914e3bfb0844dcdc31cbe2a93f894ac1cf4f38c9402a17d98f56e2d376",
        "betterdays/wrappers/ClientLevelWrapper.class","ff2facbaac7fd0529a1e9750ce1b744c75de9794a0a26d7c1cfd8e370b47ef9a");
    static boolean matches(Map<String,byte[]> classes) {
        try {
            for(var entry:PINNED.entrySet()) {
                byte[] bytes=classes.get(entry.getKey());
                if(bytes==null||!HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)).equals(entry.getValue()))return false;
            }
            return true;
        }catch(Exception e){return false;}
    }
    public void onLoad(String pkg) {
        try {
            var loader=FabricLoader.getInstance();var mod=loader.getModContainer("betterdays");
            enabled=!System.getProperty("prominence.betterDaysLifecycle","true").equalsIgnoreCase("false")
                &&loader.getModContainer("minecraft").map(m->m.getMetadata().getVersion().getFriendlyString().equals("1.20.1")).orElse(false)
                &&mod.isPresent()&&mod.get().getMetadata().getVersion().getFriendlyString().equals("3.3.4.5");
            if(enabled) {
                Map<String,byte[]> classes=new java.util.HashMap<>();
                for(String name:PINNED.keySet()) {
                    var path=mod.get().findPath(name);
                    if(path.isPresent())classes.put(name,Files.readAllBytes(path.get()));
                }
                enabled=matches(classes);
            }
        }catch(Exception e){enabled=false;}
        LoggerFactory.getLogger("Prominence compatibility").info("Client BetterDays world lifetime: {} (exact native guards)",enabled?"enabled":"disabled");
    }
    public boolean shouldApplyMixin(String target,String mixin){return enabled;}
    public String getRefMapperConfig(){return null;}
    public List<String> getMixins(){return null;}
    public void acceptTargets(Set<String> mine,Set<String> others){}
    public void preApply(String target,ClassNode node,String name,IMixinInfo info){}
    public void postApply(String target,ClassNode node,String name,IMixinInfo info) {
        int changed=BetterDaysLifecycleRewrite.rewrite(node);
        if(changed==2)LoggerFactory.getLogger("Prominence compatibility").info("Client BetterDays world lifetime: corrected 2 obsolete-world registration calls");
        else LoggerFactory.getLogger("Prominence compatibility").warn("Client BetterDays lifecycle shape changed; retaining native handlers");
    }
}
