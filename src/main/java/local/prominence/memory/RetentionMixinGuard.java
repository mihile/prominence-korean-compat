package local.prominence.memory;

import java.util.*;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.*;

public final class RetentionMixinGuard implements IMixinConfigPlugin {
    private boolean azure, screens, hud, armor;
    public void onLoad(String pkg) {
        azure=WorldCacheGuard.installed("retentionAzure");screens=WorldCacheGuard.installed("retentionScreens");hud=WorldCacheGuard.installed("retentionHud");
        armor=WorldCacheGuard.installed("retentionArmorRenderers");
        org.slf4j.LoggerFactory.getLogger("Prominence compatibility").info("Client frame-scoped spell container reads: {}",hud?"enabled":"disabled");
    }
    public boolean shouldApplyMixin(String target,String mixin) { return mixin.endsWith("AzureOwnerMixin")?azure:mixin.endsWith("PreviousScreenMixin")?screens:mixin.endsWith("ArmorRendererOwnerMixin")?armor:hud; }
    public String getRefMapperConfig(){return null;}
    public List<String> getMixins(){return null;}
    public void acceptTargets(Set<String>a,Set<String>b){}
    public void preApply(String t,ClassNode n,String m,IMixinInfo i){}
    public void postApply(String t,ClassNode n,String m,IMixinInfo i){}
}
