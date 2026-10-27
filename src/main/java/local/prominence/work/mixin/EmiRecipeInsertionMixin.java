package local.prominence.work.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.stack.EmiStack;
import java.util.Map;
import java.util.Set;
import local.prominence.work.RecipeIndexInsertion;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(targets="dev.emi.emi.registry.EmiRecipes$Manager", remap=false)
public abstract class EmiRecipeInsertionMixin {
    @WrapMethod(method={"lambda$new$3", "lambda$new$6", "lambda$new$8"}, remap=false)
    private static void prominence$reuseExistingRecipeBucket(Map<EmiStack, Set<EmiRecipe>> map,
            EmiRecipe recipe, EmiStack stack, Operation<Void> original) {
        if (!RecipeIndexInsertion.addExisting(map, recipe, stack)) original.call(map, recipe, stack);
    }
}
