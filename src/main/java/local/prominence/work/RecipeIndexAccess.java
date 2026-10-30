package local.prominence.work;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.stack.EmiStack;
import java.util.List;
import java.util.Map;

/** Read-only access to the already completed native relationship index. */
public interface RecipeIndexAccess {
    Map<EmiStack,List<EmiRecipe>> prominence$inputs();
    Map<EmiStack,List<EmiRecipe>> prominence$outputs();
}
