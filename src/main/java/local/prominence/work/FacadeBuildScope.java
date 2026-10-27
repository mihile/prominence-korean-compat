package local.prominence.work;

import java.util.IdentityHashMap;
import java.util.function.Supplier;

/** Only the native AE2 creative-tab invocation owns this set; no stacks or worlds are retained. */
public final class FacadeBuildScope implements AutoCloseable {
    private static final ThreadLocal<FacadeBuildScope> CURRENT = new ThreadLocal<>();
    private final FacadeBuildScope parent;
    private final IdentityHashMap<Object, Boolean> seen = new IdentityHashMap<>();
    private final long started = System.nanoTime();
    private boolean closed;
    private long candidates, repeated;

    private FacadeBuildScope() { parent = CURRENT.get(); CURRENT.set(this); }
    public static FacadeBuildScope begin() { return new FacadeBuildScope(); }
    public static <T> T first(Object item, Supplier<T> original, T duplicate) {
        var scope = CURRENT.get();
        if (scope == null || item == null) return original.get();
        scope.candidates++;
        if (scope.seen.containsKey(item)) { scope.repeated++; return duplicate; }
        T result = original.get(); // An exception must not mark this candidate as processed.
        scope.seen.put(item, Boolean.TRUE);
        return result;
    }
    public long candidates() { return candidates; }
    public long repeated() { return repeated; }
    public double elapsedMillis() { return (System.nanoTime() - started) / 1_000_000.0; }
    public void close() {
        if (closed) return;
        if (CURRENT.get() != this) throw new IllegalStateException("Facade scope closed out of order");
        closed = true;
        seen.clear();
        if (parent == null) CURRENT.remove(); else CURRENT.set(parent);
    }
}
