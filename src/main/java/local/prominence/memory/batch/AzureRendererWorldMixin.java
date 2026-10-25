package local.prominence.memory.batch;
import local.prominence.memory.*;
import net.minecraft.class_1297;
import net.minecraft.class_638;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(targets="mod.azure.azurelib.renderer.GeoEntityRenderer",remap=false)
public abstract class AzureRendererWorldMixin implements AzureRendererOwner {
    @Shadow protected class_1297 animatable;
    @Inject(method="<init>",at=@At("RETURN"),remap=false)
    private void prominence$trackOwner(CallbackInfo ci){AzureRendererTracker.track(this);}
    public boolean prominence$clearObsoleteWorld(Object currentWorld) {
        if(animatable!=null&&animatable.method_37908() instanceof class_638&&animatable.method_37908()!=currentWorld){animatable=null;return true;}
        return false;
    }
}
