package local.prominence.work;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.class_2960;

/** Caches only a successfully parsed immutable identifier; registry lookup remains live. */
public final class ParsedItemId {
    private record Entry(String source,class_2960 id) {}
    private volatile Entry entry;
    public class_2960 resolve(String text,Operation<class_2960> parse) {
        Entry e=entry;
        if(text!=null && e!=null && e.source()==text)return e.id();
        class_2960 result=parse.call(text);
        if(text!=null && result!=null && result.getClass()==class_2960.class)entry=new Entry(text,result);
        return result;
    }
}
