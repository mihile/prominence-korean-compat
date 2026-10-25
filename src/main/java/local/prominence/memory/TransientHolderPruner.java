package local.prominence.memory;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Predicate;

/** Owns neither entities nor their worlds; predicates and snapshots are call-local. */
final class TransientHolderPruner {
    private TransientHolderPruner() {}
    static int clearCaptured(Object manager,Field captured,Predicate<Object> obsolete) throws IllegalAccessException {
        Object event=captured.get(manager);
        if(event==null||!obsolete.test(event)||captured.get(manager)!=event)return 0;
        captured.set(manager,null);return 1;
    }
    static int removeObsolete(ConcurrentMap<Object,Object> progress,Predicate<Object> obsolete) {
        int count=0;
        for(var entry:progress.entrySet()) {
            Object key=entry.getKey(),value=entry.getValue();
            if(obsolete.test(key)&&progress.remove(key,value))count++;
        }
        return count;
    }
}
