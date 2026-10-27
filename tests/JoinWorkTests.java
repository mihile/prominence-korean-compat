package local.prominence.join;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;

public final class JoinWorkTests {
    private static int checks;
    private static void check(boolean ok,String msg) { if(!ok)throw new AssertionError(msg);checks++; }
    public static void main(String[] args)throws Exception {
        MaterialMapCapacity.completed(0);check(MaterialMapCapacity.expected()==16,"empty map hint");
        MaterialMapCapacity.completed(50_000);check(MaterialMapCapacity.expected()==50_000,"actual completed size");
        MaterialMapCapacity.completed(Integer.MAX_VALUE);check(MaterialMapCapacity.expected()==131_072,"bounded hint");
        MaterialMapCapacity.completed(-1);check(MaterialMapCapacity.expected()==16,"minimum hint");
        var original=new Object2IntLinkedOpenHashMap<Object>();var sized=new Object2IntLinkedOpenHashMap<Object>(50_000);
        Object[] keys=new Object[50_000];for(int i=0;i<keys.length;i++){keys[i]=new Object();original.putIfAbsent(keys[i],i);sized.putIfAbsent(keys[i],i);}
        for(int i=0;i<keys.length;i++){original.putIfAbsent(keys[i],-1);sized.putIfAbsent(keys[i],-1);}
        check(original.equals(sized),"same IDs and first-match precedence");
        check(original.keySet().stream().toList().equals(sized.keySet().stream().toList()),"same insertion order");
        check(original.defaultReturnValue()==sized.defaultReturnValue(),"same missing default");
        for(int n=0;n<40;n++) {
            AtomicBoolean queued=new AtomicBoolean(),killed=new AtomicBoolean();CountDownLatch entered=new CountDownLatch(1),ready=new CountDownLatch(1);
            Thread t=new Thread(()->{entered.countDown();boolean dead=IdleSongWait.await(ready,queued::get,killed::get);if(!dead&&queued.get())ready.countDown();});
            // The permit can arrive before the waiter parks; the queue predicate still prevents loss.
            if(n%2==0){queued.set(true);IdleSongWait.signal(t);}
            t.start();check(entered.await(2,TimeUnit.SECONDS),"waiter entered");
            if(n%2!=0){Thread.sleep(2);queued.set(true);IdleSongWait.signal(t);}
            check(ready.await(2,TimeUnit.SECONDS),"queued song wakes");t.join(2000);check(!t.isAlive(),"no stranded waiter");
        }
        AtomicBoolean killed=new AtomicBoolean();Thread t=new Thread(()->IdleSongWait.await(killed,()->false,killed::get));t.start();Thread.sleep(10);killed.set(true);t.interrupt();t.join(2000);check(!t.isAlive(),"kill+interrupt terminates waiting");
        check(!IdleSongWait.await(new Object(),()->true,()->false),"ready song passes immediately");
        check(IdleSongWait.await(new Object(),()->false,()->true),"already killed exits immediately");
        System.out.println("PASS: join work helper checks="+checks);
    }
}
