package local.prominence.work;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
/** Nonblocking admission for only Voxy section-mesh jobs during EMI initial work. */
public final class VoxyMeshAdmission {
 private static final int LIMIT=4;
 private static final AtomicInteger RUNNING=new AtomicInteger();
 private static final AtomicLong DEFERRED=new AtomicLong(), BLOCKED=new AtomicLong();
 private VoxyMeshAdmission(){}
 public static boolean allowed(){boolean ok=!InitialWork.active() || RUNNING.get()<LIMIT;if(!ok)BLOCKED.incrementAndGet();return ok;}
 public static boolean enter(){
  while(true){int n=RUNNING.get();if(InitialWork.active() && n>=LIMIT){DEFERRED.incrementAndGet();return false;}if(RUNNING.compareAndSet(n,n+1))return true;}
 }
 public static void leave(){if(RUNNING.decrementAndGet()<0)throw new IllegalStateException("Voxy mesh admission imbalance");}
 public static long takeDeferred(){return DEFERRED.getAndSet(0);}
 public static long takeBlocked(){return BLOCKED.getAndSet(0);}
}
