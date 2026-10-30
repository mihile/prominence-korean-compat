package local.prominence.work.mixin;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.stack.EmiStack;
import java.util.List;
import java.util.Map;
import local.prominence.work.RecipeIndexAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets="dev.emi.emi.registry.EmiRecipes$Manager",remap=false)
public interface EmiRecipeIndexAccessMixin extends RecipeIndexAccess {
    @Accessor("byInput") Map<EmiStack,List<EmiRecipe>> prominence$inputs();
    @Accessor("byOutput") Map<EmiStack,List<EmiRecipe>> prominence$outputs();
}
