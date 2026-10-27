package local.prominence.memory;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Set;
import java.util.function.DoubleSupplier;
import java.util.function.Predicate;
import mod.azure.azurelib.core.molang.LazyVariable;
import mod.azure.azurelib.core.molang.MolangQueries;

/** Cold, transition-only pruning. Preserve LazyVariable identity because parsed animations reference it. */
final class AzureQueryPruner {
    private static final DoubleSupplier ZERO = () -> 0;
    private final Class<?> modelOrigin, memoized;
    private final Field valueSupplier, originalSupplier;
    private final Set<String> names = Set.of(
        MolangQueries.ACTOR_COUNT, MolangQueries.MOON_PHASE, MolangQueries.DISTANCE_FROM_CAMERA,
        MolangQueries.IS_ON_GROUND, MolangQueries.IS_IN_WATER, MolangQueries.IS_IN_WATER_OR_RAIN,
        MolangQueries.IS_ON_FIRE, MolangQueries.HEALTH, MolangQueries.MAX_HEALTH,
        MolangQueries.GROUND_SPEED, MolangQueries.YAW_SPEED);

    AzureQueryPruner(Class<?> modelOrigin) throws ReflectiveOperationException {
        this.modelOrigin = modelOrigin;
        memoized = Class.forName("mod.azure.azurelib.core.molang.MolangParser$1", false, AzureQueryPruner.class.getClassLoader());
        valueSupplier = LazyVariable.class.getDeclaredField("valueSupplier");
        originalSupplier = memoized.getDeclaredField("val$value");
        for (Field field : new Field[]{valueSupplier, originalSupplier}) {
            if (Modifier.isStatic(field.getModifiers()) || field.getType() != DoubleSupplier.class)
                throw new IllegalStateException("Unknown AzureLib query shape");
            field.setAccessible(true);
        }
        if (Modifier.isFinal(valueSupplier.getModifiers()) || !Modifier.isFinal(originalSupplier.getModifiers()))
            throw new IllegalStateException("Unknown AzureLib supplier lifetime");
    }

    int release(Map<String, LazyVariable> variables, Predicate<Object> obsolete) throws ReflectiveOperationException {
        int released = 0;
        for (String name : names) {
            LazyVariable variable = variables.get(name);
            if (variable == null || variable.getClass() != LazyVariable.class) continue;
            Object memo = valueSupplier.get(variable);
            if (memo == null || memo.getClass() != memoized) continue;
            Object supplier = originalSupplier.get(memo);
            if (supplier == null || !supplier.getClass().isHidden()
                || supplier.getClass().getNestHost() != modelOrigin) continue;
            boolean old = false;
            for (Field captured : supplier.getClass().getDeclaredFields()) {
                if (Modifier.isStatic(captured.getModifiers()) || captured.getType().isPrimitive()) continue;
                captured.setAccessible(true);
                if (obsolete.test(captured.get(supplier))) { old = true; break; }
            }
            // Run only on the client thread after transitions. Check the exact slot again;
            // never clear another mod's replacement, or remove/recreate parsed variables.
            if (old && variables.get(name) == variable && valueSupplier.get(variable) == memo) {
                variable.set(ZERO);
                released++;
            }
        }
        return released;
    }
}
