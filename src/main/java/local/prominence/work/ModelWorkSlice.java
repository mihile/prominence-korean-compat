package local.prominence.work;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/** A cooperative boundary between complete models; does not touch queues or GL state. */
public final class ModelWorkSlice implements AutoCloseable {
    private static final ThreadLocal<ModelWorkSlice> CURRENT = new ThreadLocal<>();
    private static final AtomicLong YIELDS = new AtomicLong();
    private static final AtomicInteger RENDER_GROUP_ACTIVE = new AtomicInteger();
    private static final long BUDGET_NS = 4_000_000L;
    private final ModelWorkSlice previous;
    private final long start;
    private boolean closed;
    private ModelWorkSlice(long start) { this.previous=CURRENT.get(); this.start=start; CURRENT.set(this); }
    public static ModelWorkSlice begin() { return new ModelWorkSlice(System.nanoTime()); }
    static ModelWorkSlice begin(long start) { return new ModelWorkSlice(start); }
    public static void enterRenderGroup() { RENDER_GROUP_ACTIVE.incrementAndGet(); }
    public static void exitRenderGroup() {
        int next = RENDER_GROUP_ACTIVE.decrementAndGet();
        if (next < 0) RENDER_GROUP_ACTIVE.set(0);
    }
    public static boolean isRenderGroupActive() { return RENDER_GROUP_ACTIVE.get() > 0; }
    public static boolean shouldDeferToRenderGroup() {
        if (RENDER_GROUP_ACTIVE.get() <= 0) return false;
        YIELDS.incrementAndGet();
        return true;
    }
    public static boolean continueBatch(boolean more) { return continueBatch(more, System.nanoTime()); }
    static boolean continueBatch(boolean more, long now) {
        var slice=CURRENT.get();
        if (!more || slice==null) return more;
        if (RENDER_GROUP_ACTIVE.get() <= 0 && now-slice.start < BUDGET_NS) return true;
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
