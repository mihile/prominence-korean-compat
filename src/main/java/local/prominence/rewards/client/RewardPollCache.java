package local.prominence.rewards.client;
import java.lang.ref.WeakReference;import java.util.Arrays;import java.util.function.Supplier;
/** Bounded polling and packet reuse without retaining a player/world. */
public final class RewardPollCache<T> {
 private WeakReference<Object> owner=new WeakReference<>(null);private T value;private long at;private byte[] packet;
 public synchronized T poll(Object key,long now,Supplier<T> load){if(owner.get()!=key||value==null||now-at>=250_000_000L||now<at){value=load.get();owner=new WeakReference<>(key);at=now;}return value;}
 public synchronized boolean duplicate(Object key,byte[] bytes){return owner.get()==key&&packet!=null&&Arrays.equals(packet,bytes);}
 public synchronized void accepted(Object key,byte[] bytes){owner=new WeakReference<>(key);packet=bytes;}
 public synchronized void clear(){owner.clear();value=null;packet=null;}
}
