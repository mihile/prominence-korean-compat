package local.prominence.work.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import local.prominence.work.EmiStackBuildScope;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(targets = "dev.emi.emi.registry.EmiRecipes$Worker", remap = false)
public abstract class EmiSortedPreparationScopeMixin {
    @WrapMethod(method = "run", remap = false)
    private void prominence$prepareNativeStacks(Operation<Void> original) {
        try (var scope = EmiStackBuildScope.begin("sorted-recipe-worker")) { original.call(); }
    }
}
