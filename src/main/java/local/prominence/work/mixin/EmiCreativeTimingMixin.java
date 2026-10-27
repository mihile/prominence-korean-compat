package local.prominence.work.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import java.util.Map;
import net.minecraft.class_1761;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;

/** Time the original callback without changing item ordering, thread or reload behavior. */
@Mixin(targets="dev.emi.emi.registry.EmiStackList", remap=false)
public abstract class EmiCreativeTimingMixin {
    @WrapMethod(method="lambda$reload$1", remap=false)
    private static void prominence$timeGroup(class_1761.class_8128 context, Map<?, ?> stacks,
                                             class_1761 group, Operation<Void> original) {
        long started = System.nanoTime();
        try { original.call(context, stacks, group); }
        finally {
            double millis = (System.nanoTime() - started) / 1_000_000.0;
            if (millis >= 20) {
                // Diagnostics must not replace the native callback's exception handling.
                try { LoggerFactory.getLogger("Prominence compatibility").info(
                    "EMI creative group: name={}, elapsedMs={}", group.method_7737().getString(), millis); }
                catch (RuntimeException ignored) { }
            }
        }
    }
}
