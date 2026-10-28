package local.prominence.work;

import org.slf4j.LoggerFactory;

/** Only historical mapping loading defers work. Newly ingested states retain native construction. */
public final class MapperLoadScope implements AutoCloseable {
    private static final ThreadLocal<MapperLoadScope> CURRENT = new ThreadLocal<>();
    private final MapperLoadScope previous;
    private final long started = System.nanoTime();
    private int deferred;
    private boolean closed;
    private MapperLoadScope() { previous = CURRENT.get(); CURRENT.set(this); }
    public static MapperLoadScope begin() { return new MapperLoadScope(); }
    public static boolean defer() {
        MapperLoadScope scope = CURRENT.get();
        if (scope == null) return false;
        scope.deferred++;
        return true;
    }
    public void close() {
        if (closed) return;
        closed = true;
        if (previous == null) CURRENT.remove(); else CURRENT.set(previous);
        LoggerFactory.getLogger("Prominence compatibility").info(
            "Voxy saved mapping load: {} ms, {} opacity calculations deferred until use",
            (System.nanoTime() - started) / 1_000_000.0, deferred);
    }
}
