package local.prominence.memory.batch;
import local.prominence.memory.ArmorWorldContexts;
import net.minecraft.class_970;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=class_970.class,priority=500)
public abstract class ArmorLayerTrackingMixin {
    @Inject(method="<init>",at=@At("RETURN"),remap=false)
    private void prominence$trackArmorContext(CallbackInfo ci){ArmorWorldContexts.track(this);}
}
