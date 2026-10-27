package local.prominence.work;

import java.util.concurrent.atomic.AtomicLong;

/** A cooperative boundary between complete models; does not touch queues or GL state. */
public final class ModelWorkSlice implements AutoCloseable {
    private static final ThreadLocal<ModelWorkSlice> CURRENT = new ThreadLocal<>();
    private static final AtomicLong YIELDS = new AtomicLong();
    private static final long BUDGET_NS = 8_000_000L;
    private final ModelWorkSlice previous;
    private final long start;
    private boolean closed;
    private ModelWorkSlice(long start) { this.previous=CURRENT.get(); this.start=start; CURRENT.set(this); }
    public static ModelWorkSlice begin() { return new ModelWorkSlice(System.nanoTime()); }
    static ModelWorkSlice begin(long start) { return new ModelWorkSlice(start); }
    public static boolean continueBatch(boolean more) { return continueBatch(more, System.nanoTime()); }
    static boolean continueBatch(boolean more, long now) {
        var slice=CURRENT.get();
        if (!more || slice==null || now-slice.start < BUDGET_NS || !InitialWork.active()) return more;
        YIELDS.incrementAndGet();
        return false;
    }
    public static long takeYields() { return YIELDS.getAndSet(0); }
    @Override public void close() {
        if (closed) return;
        if (CURRENT.get()!=this) throw new IllegalStateException("Model slice closed out of order");
        closed=true;
        if (previous==null) CURRENT.remove(); else CURRENT.set(previous);
    }
}
