package local.prominence.memory;

import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;

/** Preserve parsed variable identity; detach only a pinned GeoModel's obsolete captures. */
final class ArmorQueryPruner {
    private static final DoubleSupplier ZERO=()->0;
    private final Class<?> model,variable,memo;
    private final Field supplier,original;
    private final Map<?,?> variables;
    ArmorQueryPruner() throws ReflectiveOperationException {
        ClassLoader loader=getClass().getClassLoader();String prefix="mod.azure.azurelibarmor.";
        model=Class.forName(prefix+"model.GeoModel",false,loader);
        variable=Class.forName(prefix+"core.molang.LazyVariable",false,loader);
        memo=Class.forName(prefix+"core.molang.MolangParser$1",false,loader);
        supplier=RetentionCleanupClient.field(variable,"valueSupplier",false,DoubleSupplier.class,true);
        original=RetentionCleanupClient.field(memo,"val$value",false,DoubleSupplier.class,false);
        Class<?> parser=Class.forName(prefix+"core.molang.MolangParser",false,loader);
        variables=(Map<?,?>)RetentionCleanupClient.field(parser,"VARIABLES",true,Map.class,false).get(null);
    }
    int release(Predicate<Object> obsolete) throws ReflectiveOperationException {
        int removed=0;
        // Native model-owned memoized queries only, not variables from other mods.
        for(Object value:new ArrayList<>(variables.values())) {
            if(value==null||value.getClass()!=variable)continue;
            Object wrapper=supplier.get(value);if(wrapper==null||wrapper.getClass()!=memo)continue;
            Object lambda=original.get(wrapper);
            if(lambda==null||!lambda.getClass().isHidden()||lambda.getClass().getNestHost()!=model)continue;
            boolean old=false;
            for(Field capture:lambda.getClass().getDeclaredFields()) {
                if(Modifier.isStatic(capture.getModifiers())||capture.getType().isPrimitive())continue;
                capture.setAccessible(true);if(obsolete.test(capture.get(lambda))){old=true;break;}
            }
            if(old&&supplier.get(value)==wrapper){supplier.set(value,ZERO);removed++;}
        }
        return removed;
    }
}
