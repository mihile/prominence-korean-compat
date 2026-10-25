package local.prominence.memory;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Predicate;
import net.minecraft.class_1309;
import net.minecraft.class_638;

/** Weakly track layers; only expired client rendering contexts are restored to constructor state. */
public final class ArmorWorldContexts {
    private ArmorWorldContexts(){}
    private static final WeakListenerSet<Object> LAYERS=new WeakListenerSet<>();
    private static final ClassValue<List<Field>> FIELDS=new ClassValue<>() {
        protected List<Field> computeValue(Class<?> type) {
            List<Field> result=new ArrayList<>();
            for(Class<?> c=type;c!=null;c=c.getSuperclass())for(Field field:c.getDeclaredFields())
                if(!Modifier.isStatic(field.getModifiers())&&(field.getName().contains("gl_storedEntity")||field.getName().contains("gl_storedSlot")||field.getName().contains("gl_storedItemStack"))) {field.setAccessible(true);result.add(field);}
            return List.copyOf(result);
        }
    };
    public static void track(Object layer){LAYERS.add(layer);}
    public static int prune(Object layer,Predicate<Object> obsolete)throws IllegalAccessException {
        var fields=FIELDS.get(layer.getClass());int cleared=0;boolean remainingOwner=false;
        for(Field field:fields)if(field.getName().contains("gl_storedEntity")) {
            Object owner=field.get(layer);
            if(owner!=null&&obsolete.test(owner)){field.set(layer,null);cleared++;}else if(owner!=null)remainingOwner=true;
        }
        if(cleared>0&&!remainingOwner)for(Field field:fields)if(!field.getName().contains("gl_storedEntity"))field.set(layer,null);
        return cleared;
    }
    public static int release(Object currentWorld)throws IllegalAccessException {
        int total=0;for(Object layer:LAYERS.snapshot())total+=prune(layer,owner->owner instanceof class_1309 e&&e.method_37908() instanceof class_638&&e.method_37908()!=currentWorld);return total;
    }
}
