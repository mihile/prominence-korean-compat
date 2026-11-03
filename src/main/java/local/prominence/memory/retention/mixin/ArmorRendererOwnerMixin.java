package local.prominence.memory.retention.mixin;
import local.prominence.memory.*;
import net.minecraft.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** prepForRender repopulates all these fields before the next armor render. */
@Mixin(targets={"mod.azure.azurelib.renderer.GeoArmorRenderer","mod.azure.azurelibarmor.renderer.GeoArmorRenderer"},remap=false)
public abstract class ArmorRendererOwnerMixin implements RetainedClientOwner {
    @Shadow(remap=false) protected class_1297 currentEntity;
    @Shadow(remap=false) protected class_1799 currentStack;
    @Shadow(remap=false) protected class_1304 currentSlot;
    @Shadow(remap=false) protected class_572<?> baseModel;
    @Inject(method="<init>",at=@At("RETURN"))
    private void prominence$track(CallbackInfo ci){RetentionCleanupClient.trackArmor(this);}
    public Object prominence$retainedOwner(){return currentEntity;}
    public void prominence$releaseOwner(){currentEntity=null;currentStack=null;currentSlot=null;baseModel=null;}
}
