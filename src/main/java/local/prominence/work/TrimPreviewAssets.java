package local.prominence.work;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.function.Predicate;
import net.minecraft.class_2960;
import net.minecraft.class_3300;
import org.slf4j.LoggerFactory;

/** Validate source masks before Dynamic Trim creates material/model permutations. */
public final class TrimPreviewAssets {
    private TrimPreviewAssets() {}
    private static WeakReference<class_3300> resources=new WeakReference<>(null);
    private static List<class_2960> source,cached;
    public static synchronized void beginReload(class_3300 manager){resources=new WeakReference<>(manager);source=null;cached=null;}
    public static synchronized List<class_2960> supported(List<class_2960> ids){
        class_3300 manager=resources.get();if(manager==null)return ids;
        if(source==ids&&cached!=null)return cached;
        var result=new ArrayList<class_2960>();var missing=new ArrayList<String>();
        for(var id:ids){
            if(hasMasks(id.toString(),path->manager.method_14486(new class_2960(path)).isPresent()))result.add(id);
            else missing.add(id.toString());
        }
        source=ids;cached=List.copyOf(result);
        if(!missing.isEmpty())LoggerFactory.getLogger("Prominence compatibility").info(
            "Trim item previews: {} patterns lack source masks; use base item icons for {}. Worn armor trims and recipes are unchanged.",missing.size(),missing);
        return cached;
    }
    public static boolean hasMasks(String pattern,Predicate<String> exists){
        String encoded=pattern.replace(':','-');
        for(String slot:List.of("helmet","chestplate","leggings","boots"))
            if(!exists.test("minecraft:textures/trims/items/"+slot+"/"+encoded+".png"))return false;
        return true;
    }
}
