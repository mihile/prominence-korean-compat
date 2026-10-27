package local.prominence.join;

import java.util.concurrent.locks.LockSupport;
import java.util.function.BooleanSupplier;

/** Sleeps only between playback attempts; never runs inside the audio decoder. */
public final class IdleSongWait {
    private IdleSongWait() {}
    public static boolean await(Object blocker, BooleanSupplier queued, BooleanSupplier killed) {
        while (!killed.getAsBoolean() && !queued.getAsBoolean()) {
            // forceKill interrupts before publishing kill. Return to its native loop in that case.
            if (Thread.currentThread().isInterrupted()) return killed.getAsBoolean();
            LockSupport.parkNanos(blocker, 2_000_000L);
        }
        return killed.getAsBoolean();
    }
    public static void signal(Thread thread) { LockSupport.unpark(thread); }
}
