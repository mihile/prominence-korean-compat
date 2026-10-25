package local.prominence.memory;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Predicate;

/** Main-client-thread cache eviction only. No stop callbacks, item mutation or persistent owner list. */
final class WorldCacheReleaser {
    static int remove(Map<Object,Object> map,Predicate<Map.Entry<Object,Object>> obsolete) {
        var snapshot=new ArrayList<Map.Entry<Object,Object>>();
        for(var e:map.entrySet())snapshot.add(new AbstractMap.SimpleImmutableEntry<>(e.getKey(),e.getValue()));
        int count=0;
        for(var e:snapshot)if(obsolete.test(e)&&map.get(e.getKey())==e.getValue()&&map.remove(e.getKey(),e.getValue()))count++;
        return count;
    }
    static boolean anyObsolete(Object value,Predicate<Object> obsolete) {
        if(!(value instanceof List<?> list))throw new IllegalStateException("Unknown preview entity-list shape");
        for(Object owner:list)if(owner!=null&&obsolete.test(owner))return true;
        return false;
    }
    static int clearPair(Field owner,Field item,Predicate<Object> obsolete) throws IllegalAccessException {
        Object observed=owner.get(null),key=item.get(null);
        if(observed==null||!obsolete.test(observed)||owner.get(null)!=observed||item.get(null)!=key)return 0;
        item.set(null,null);owner.set(null,null);return 1;
    }
    static int clearLocal(ThreadLocal<?> local,Predicate<Object> obsolete) {
        Object observed=local.get();
        if(observed==null){local.remove();return 0;}
        if(!obsolete.test(observed)||local.get()!=observed)return 0;
        local.remove();return 1;
    }
}
