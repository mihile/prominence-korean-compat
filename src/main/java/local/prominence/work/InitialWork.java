package local.prominence.work;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.LoggerFactory;

/** Coordinates only deferred terrain submissions while EMI initializes. No world/stack references. */
public final class InitialWork {
    private static final AtomicInteger ACTIVE=new AtomicInteger();
    private static final AtomicLong LIMITED=new AtomicLong();
    private InitialWork() {}
    public static Scope begin() { Scope scope=new Scope();ACTIVE.incrementAndGet();return scope; }
    public static boolean active() { return ACTIVE.get()>0; }
    public static int budget(int original,int threads,int busy,int queued) {
        if (original<=0 || threads<4 || ACTIVE.get()==0) return original;
        int target=Math.max(2,threads/2);
        int adjusted=(int)Math.max(0L,Math.min((long)original,(long)target-Math.max(0,busy)-Math.max(0,queued)));
        if(adjusted<original)LIMITED.incrementAndGet();
        return adjusted;
    }
    public static final class Scope implements AutoCloseable {
        private boolean closed;
        private Scope() {}
        public synchronized void close() {
            if(closed)return;closed=true;
            if(ACTIVE.decrementAndGet()==0)LoggerFactory.getLogger("Prominence compatibility").info(
                "EMI initial terrain coordination finished: limitedBudgetCalls={} (native scheduling restored)",LIMITED.getAndSet(0));
        }
    }
}
