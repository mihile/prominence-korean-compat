package local.prominence.work.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import local.prominence.work.InitialWork;
import org.spongepowered.asm.mixin.Mixin;

/** Throttles MapWriter.onRender during EMI initial work to prevent render-thread lock stalls. */
@Mixin(targets="xaero.map.MapWriter", remap=false)
public abstract class XaeroMapWriterTimingMixin {
    private static long prominence$lastOnRenderTimeMs = 0L;

    @WrapMethod(method="onRender", remap=false)
    private void prominence$throttleOnRender(
            Object biomeColorCalculator, Object overlayManager, Operation<Void> original) {
        if (InitialWork.active()) {
            long now = System.currentTimeMillis();
            if (now - prominence$lastOnRenderTimeMs < 250L) {
                return;
            }
            prominence$lastOnRenderTimeMs = now;
        }
        original.call(biomeColorCalculator, overlayManager);
    }
}
