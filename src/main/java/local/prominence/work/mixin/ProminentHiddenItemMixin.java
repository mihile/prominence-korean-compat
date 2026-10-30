package local.prominence.work.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.ItemEmiStack;
import elocindev.prominent.registry.ItemRegistry;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(targets = "elocindev.prominent.plugin.ProminentEMIPlugin", remap = false)
public abstract class ProminentHiddenItemMixin {
    @WrapMethod(method = "lambda$initialize$0", remap = false)
    private static boolean prominence$compareItemDirectly(EmiStack stack, Operation<Boolean> original) {
        if (stack != null && stack.getClass() == ItemEmiStack.class
                && stack.getAmount() > 0 && stack.getAmount() <= Integer.MAX_VALUE)
            return stack.getKey() == ItemRegistry.SKILLET;
        return original.call(stack);
    }
}
