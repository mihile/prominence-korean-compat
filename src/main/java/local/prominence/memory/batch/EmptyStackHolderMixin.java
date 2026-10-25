package local.prominence.memory.batch;
import net.minecraft.class_1297;
import net.minecraft.class_1799;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(class_1799.class)
public abstract class EmptyStackHolderMixin {
    @Inject(method="method_27320(Lnet/minecraft/class_1297;)V",at=@At("HEAD"),cancellable=true,remap=false)
    private void prominence$keepEmptyUnowned(class_1297 entity,CallbackInfo ci){if((Object)this==class_1799.field_8037&&entity!=null)ci.cancel();}
}
