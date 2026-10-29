package local.prominence.work;

import java.util.Collection;
import java.util.function.Function;
import java.util.function.IntFunction;

public final class StateOrder {
    private StateOrder() {}
    /** Identity, length and exact ID order must all agree. No registry hashes or cached snapshots. */
    public static <T,S> boolean identical(Iterable<T> entries,Function<T,? extends Collection<S>> states,int size,IntFunction<S> byId) {
        int index=0;
        for(T entry:entries) for(S state:states.apply(entry)) {
            if(index>=size || state==null || byId.apply(index++)!=state) return false;
        }
        return index==size;
    }
}
