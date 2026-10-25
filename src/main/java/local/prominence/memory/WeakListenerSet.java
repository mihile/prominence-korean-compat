package local.prominence.memory;

import java.util.ArrayList;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

/** Weak identity keys, registration order, and no value reference back to an entity. */
final class WeakListenerSet<T> {
    private static final class Key<T> extends WeakReference<T> {
        private final int hash;
        Key(T value,ReferenceQueue<T> queue){super(value,queue);hash=System.identityHashCode(value);}
        public int hashCode(){return hash;}
        public boolean equals(Object other){
            if(this==other)return true;
            T value=get();return value!=null && other instanceof Key<?> key && value==key.get();
        }
    }
    private final ReferenceQueue<T> queue=new ReferenceQueue<>();
    private final LinkedHashMap<Key<T>, Boolean> listeners = new LinkedHashMap<>();
    private void purge(){
        java.lang.ref.Reference<? extends T> dead;
        while((dead=queue.poll())!=null)listeners.remove(dead);
    }
    synchronized void add(T listener) {
        Objects.requireNonNull(listener);purge();
        if(!listeners.containsKey(new Key<>(listener,null)))listeners.put(new Key<>(listener,queue),Boolean.TRUE);
    }
    synchronized List<T> snapshot() {
        purge();List<T> result=new ArrayList<>(listeners.size());
        var iterator=listeners.keySet().iterator();
        while(iterator.hasNext()){
            T listener=iterator.next().get();
            if(listener==null)iterator.remove();else result.add(listener);
        }
        return result;
    }
}
