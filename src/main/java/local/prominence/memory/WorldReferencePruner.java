package local.prominence.memory;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

/** No strong world/player cache. Runs only on an actual client world/player transition. */
final class WorldReferencePruner {
    private WeakReference<Object> world=new WeakReference<>(null),player=new WeakReference<>(null);
    private boolean initialized;
    boolean transition(Object nextWorld,Object nextPlayer) {
        boolean changed=initialized?(world.get()!=nextWorld||player.get()!=nextPlayer):(nextWorld!=null||nextPlayer!=null);
        initialized=true;
        if(changed){world=new WeakReference<>(nextWorld);player=new WeakReference<>(nextPlayer);}
        return changed;
    }
    static <K,V> int removeStale(Map<K,V> map,Predicate<K> keep,Consumer<V> stop) {
        int removed=0;
        for(var entry:new ArrayList<>(map.entrySet())){
            K key=entry.getKey();V value=entry.getValue();
            if(keep.test(key)||map.get(key)!=value)continue;
            // Unlink before stopping: a stop callback may re-enter the mod's map.
            map.remove(key);removed++;if(value!=null)stop.accept(value);
        }
        return removed;
    }
    static boolean obsoleteOwner(Object owner,Object ownerWorld,Object currentPlayer,Object currentWorld) {
        return owner!=currentPlayer||ownerWorld!=currentWorld;
    }
}
