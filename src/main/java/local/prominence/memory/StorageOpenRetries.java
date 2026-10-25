package local.prominence.memory;

import java.lang.ref.WeakReference;
import java.util.LinkedHashMap;
import java.util.WeakHashMap;
import java.util.function.Predicate;
import java.util.function.Supplier;

/** Client storage open failures only. Never forces a file lock or retains a client world. */
public final class StorageOpenRetries {
    public interface Events {
        void failed(int failures,boolean exhausted,RuntimeException failure);
        void recovered();
    }
    private static final long[] DELAYS={250_000_000L,1_000_000_000L,2_000_000_000L,4_000_000_000L,8_000_000_000L,16_000_000_000L};
    private static final int LIMIT=DELAYS.length+1;
    private static final class State {
        final WeakReference<Object> context;
        int failures;long next;boolean busy;
        State(Object context){this.context=new WeakReference<>(context);}
    }
    private final WeakHashMap<Object,LinkedHashMap<Object,State>> owners=new WeakHashMap<>();
    private volatile boolean hasFailures;
    private State state(Object owner,Object key,Object context) {
        var map=owners.get(owner);if(map==null)return null;var s=map.get(key);
        if(s!=null&&s.context.get()!=context){map.remove(key);s=null;}
        return s;
    }
    private void remove(Object owner,Object key,State expected) {
        var map=owners.get(owner);if(map!=null){map.remove(key,expected);if(map.isEmpty())owners.remove(owner);}
        hasFailures=!owners.isEmpty();
    }
    public boolean unavailable(Object owner,Object key,Object context) {
        if(!hasFailures)return false;synchronized(this){return state(owner,key,context)!=null;}
    }
    public boolean blocked(Object owner,Object key,Object context,long now) {
        if(!hasFailures)return false;synchronized(this){var s=state(owner,key,context);return s!=null&&(s.busy||s.failures>=LIMIT||now-s.next<0);}
    }
    public <T> T open(Object owner,Object key,Object context,long now,Supplier<T> original,Predicate<RuntimeException> isLockFailure,Events events) {
        State active=null;
        if(hasFailures)synchronized(this){
            active=state(owner,key,context);
            if(active!=null){if(active.busy||active.failures>=LIMIT||now-active.next<0)return null;active.busy=true;}
        }
        try {
            T result=original.get();
            if(active!=null)synchronized(this){active.busy=false;if(result!=null)remove(owner,key,active);}
            if(active!=null&&result!=null)events.recovered();
            return result;
        }catch(RuntimeException failure){
            if(!isLockFailure.test(failure)){
                if(active!=null)synchronized(this){active.busy=false;}
                throw failure;
            }
            int count;boolean exhausted;
            synchronized(this){
                var map=owners.computeIfAbsent(owner,k->new LinkedHashMap<>());
                State s=state(owner,key,context);
                if(s==null){s=new State(context);map.put(key,s);}
                s.busy=false;count=++s.failures;exhausted=count>=LIMIT;
                s.next=exhausted?now:now+DELAYS[count-1];
                // Identifiers contain registry keys and a weak engine cache, not the client world.
                while(map.size()>64)map.remove(map.keySet().iterator().next());
                hasFailures=true;
            }
            events.failed(count,exhausted,failure);return null;
        }
    }
    int ownerCount(){synchronized(this){return owners.size();}}
}
