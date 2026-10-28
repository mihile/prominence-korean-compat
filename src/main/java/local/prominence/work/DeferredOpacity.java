package local.prominence.work;

import java.util.function.IntSupplier;

/** Per-entry, exact native result. Failed calculations remain retryable; no global state cache. */
public final class DeferredOpacity {
    private volatile IntSupplier pending;
    private int value;
    public DeferredOpacity(IntSupplier pending) { this.pending = java.util.Objects.requireNonNull(pending); }
    public int get() {
        if (pending != null) synchronized (this) {
            if (pending != null) {
                value = pending.getAsInt();
                pending = null; // Publishes value and releases the native BlockGetter/closure.
            }
        }
        return value;
    }
}
