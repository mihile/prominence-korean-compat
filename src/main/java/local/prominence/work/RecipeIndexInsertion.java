package local.prominence.work;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.stack.Comparison;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.ItemEmiStack;
import dev.emi.emi.registry.EmiComparisonDefaults;
import dev.emi.emi.registry.EmiStackList;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;
import java.util.Map;
import java.util.Set;

/** A hit never retains the source stack. Missing keys still use EMI's native copy. */
public final class RecipeIndexInsertion {
    private RecipeIndexInsertion() {}
    public static boolean addExisting(Map<EmiStack, Set<EmiRecipe>> map, EmiRecipe recipe, EmiStack stack) {
        if (stack == null || stack.getClass() != ItemEmiStack.class || stack.getRemainder() != EmiStack.EMPTY
                || map.getClass() != Object2ObjectOpenCustomHashMap.class) return false;
        var strategy = ((Object2ObjectOpenCustomHashMap<?, ?>) map).strategy();
        if (strategy.getClass() != EmiStackList.ComparisonHashStrategy.class) return false;
        Comparison comparison = EmiComparisonDefaults.get(stack.getKey());
        // Arbitrary plugin hash/predicate functions may observe copies or have side effects.
        if (comparison != Comparison.DEFAULT_COMPARISON && comparison != Comparison.compareNbt()) return false;
        Set<EmiRecipe> found = map.get(stack);
        if (found == null) return false;
        found.add(recipe);
        return true;
    }
}
