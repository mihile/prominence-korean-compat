package local.prominence.memory;
import com.github.nyuppo.variant.MobVariant;
import java.lang.reflect.Field;
import java.util.Optional;
import net.minecraft.class_1297;
import net.minecraft.class_1309;
import net.minecraft.class_2487;

/** Cache field metadata only; read the current native variant at every visual lookup. */
public final class VariantProjection {
    private VariantProjection(){}
    private static final ClassValue<Optional<Field>> FIELDS=new ClassValue<>() {
        protected Optional<Field> computeValue(Class<?> type) {
            for(Class<?> c=type;c!=null;c=c.getSuperclass())try {
                Field f=c.getDeclaredField("variant");
                if(f.getType()==MobVariant.class){f.setAccessible(true);return Optional.of(f);}
            }catch(NoSuchFieldException ignored){}catch(RuntimeException e){return Optional.empty();}
            return Optional.empty();
        }
    };
    public static boolean project(Object entity,class_2487 nbt) {
        if(entity==null)return false;
        try {
            var field=FIELDS.get(entity.getClass());if(field.isEmpty())return false;
            Object value=field.get().get(entity);if(!(value instanceof MobVariant variant))return false;
            nbt.method_10582("VariantID",variant.getIdentifier().toString());return true;
        }catch(IllegalAccessException|RuntimeException e){return false;}
    }
    public static class_2487 writeFull(class_1297 entity,class_2487 nbt) {
        return project(entity,nbt)?nbt:entity.method_5647(nbt);
    }
    public static void writeCustom(class_1309 entity,class_2487 nbt) {
        if(!project(entity,nbt))entity.method_5652(nbt);
    }
}
