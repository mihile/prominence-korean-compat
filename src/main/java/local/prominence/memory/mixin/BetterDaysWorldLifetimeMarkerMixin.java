package local.prominence.memory.mixin;

import net.minecraft.class_310;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** Runs after BetterDays' priority-1000 handlers; runtime work is in native callbacks. */
@Mixin(value=class_310.class,priority=500)
public abstract class BetterDaysWorldLifetimeMarkerMixin {
    @Unique private static void prominence$betterDaysLifecycleMarker() {}
}
