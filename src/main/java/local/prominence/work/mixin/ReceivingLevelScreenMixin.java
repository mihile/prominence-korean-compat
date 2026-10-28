package local.prominence.work.mixin;

import net.minecraft.class_310;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Immediately closes the ReceivingLevelScreen (loading screen) during dimension transitions
 *  instead of stalling 5 seconds waiting for distant terrain chunks. */
@Mixin(targets="net.minecraft.class_434", remap=false)
public abstract class ReceivingLevelScreenMixin {
    @Shadow public abstract void method_25419(); // close()

    @Inject(method="method_25393", at=@At("HEAD"), cancellable=true, remap=false)
    private void prominence$instantDimensionTransition(CallbackInfo ci) {
        class_310 client = class_310.method_1551();
        if (client != null && client.field_1724 != null && client.field_1687 != null) {
            // Player and world are active in the new dimension; dismiss the loading screen immediately!
            try {
                this.method_25419();
            } catch (Throwable t) {
                client.method_1507(null);
            }
            ci.cancel();
        }
    }
}
