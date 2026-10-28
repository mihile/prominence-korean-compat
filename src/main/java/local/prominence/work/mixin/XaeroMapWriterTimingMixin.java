package local.prominence.work.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import local.prominence.work.InitialWork;
import org.spongepowered.asm.mixin.Mixin;
import xaero.map.biome.BiomeColorCalculator;
import xaero.map.region.OverlayManager;

/** Throttles MapWriter.onRender to prevent render-thread frame drops both during initial work and continuous gameplay. */
@Mixin(targets="xaero.map.MapWriter", remap=false)
public abstract class XaeroMapWriterTimingMixin {
    private static long prominence$lastOnRenderTimeMs = 0L;

    @WrapMethod(method="onRender", remap=false)
    private void prominence$throttleOnRender(
            BiomeColorCalculator biomeColorCalculator, OverlayManager overlayManager, Operation<Void> original) {
        long now = System.currentTimeMillis();
        long interval = InitialWork.active() ? 250L : 33L; // Cap at 30 map-pixel updates/sec during gameplay
        if (now - prominence$lastOnRenderTimeMs < interval) {
            return;
        }
        prominence$lastOnRenderTimeMs = now;
        original.call(biomeColorCalculator, overlayManager);
    }
}
