package local.prominence.work.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import local.prominence.work.EmiStackBuildScope;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(targets = "dev.emi.emi.registry.EmiRecipes", remap = false)
public abstract class EmiRecipePreparationScopeMixin {
    @WrapMethod(method = "bake", remap = false)
    private static void prominence$prepareNativeStacks(Operation<Void> original) {
        try (var scope = EmiStackBuildScope.begin("recipe-bake")) { original.call(); }
    }
}
