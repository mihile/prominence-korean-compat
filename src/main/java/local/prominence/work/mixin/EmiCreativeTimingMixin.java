package local.prominence.work.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import net.minecraft.class_1761;
import net.minecraft.class_310;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Time and offload creative entries to prevent 1.6s render thread stalls during join. */
@Mixin(targets="dev.emi.emi.registry.EmiStackList", remap=false)
public abstract class EmiCreativeTimingMixin {
    @WrapOperation(
        method="reload",
        at=@At(value="INVOKE", target="Lnet/minecraft/class_310;method_5385(Ljava/util/function/Supplier;)Ljava/util/concurrent/CompletableFuture;"),
        remap=false
    )
    private static CompletableFuture<?> prominence$offloadCreativeReload(
            class_310 client, Supplier<?> supplier, Operation<CompletableFuture<?>> original) {
        try {
            Object result = supplier.get();
            return CompletableFuture.completedFuture(result);
        } catch (Throwable t) {
            LoggerFactory.getLogger("Prominence compatibility").warn(
                "EMI creative group build off-thread failed, falling back to render thread: {}", t.toString());
            return original.call(client, supplier);
        }
    }

    @WrapMethod(method="lambda$reload$1", remap=false)
    private static void prominence$timeGroup(class_1761.class_8128 context, Map<?, ?> stacks,
                                             class_1761 group, Operation<Void> original) {
        long started = System.nanoTime();
        try { original.call(context, stacks, group); }
        finally {
            double millis = (System.nanoTime() - started) / 1_000_000.0;
            if (millis >= 20) {
                try { LoggerFactory.getLogger("Prominence compatibility").info(
                    "EMI creative group: name={}, elapsedMs={}", group.method_7737().getString(), millis); }
                catch (RuntimeException ignored) { }
            }
        }
    }
}
