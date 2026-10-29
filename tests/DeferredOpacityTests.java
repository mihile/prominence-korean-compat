package local.prominence.work;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public final class DeferredOpacityTests {
    public static void main(String[] args) throws Exception {
        AtomicInteger calls = new AtomicInteger();
        DeferredOpacity[] historical = new DeferredOpacity[20_000];
        for (int i=0; i<historical.length; i++) { final int id=i; historical[i]=new DeferredOpacity(()->{calls.incrementAndGet();return id%16;}); }
        if (calls.get()!=0) throw new AssertionError("history eagerly computed");
        for (int i=0;i<100;i++) if (historical[i].get()!=i%16) throw new AssertionError("native value changed");
        for (int i=0;i<100;i++) historical[i].get();
        if (calls.get()!=100) throw new AssertionError("unused work or repeated work");
        AtomicInteger concurrent = new AtomicInteger();
        var one=new DeferredOpacity(()->{concurrent.incrementAndGet();return 13;});
        var pool=Executors.newFixedThreadPool(8);
        try {
            var jobs=new java.util.ArrayList<Future<Integer>>();
            for(int i=0;i<256;i++) jobs.add(pool.submit(one::get));
            for(var job:jobs) if(job.get()!=13)throw new AssertionError("publication");
        } finally { pool.shutdownNow(); }
        if(concurrent.get()!=1)throw new AssertionError("duplicate calculation");
        AtomicInteger attempts=new AtomicInteger();
        var retry=new DeferredOpacity(()->{if(attempts.incrementAndGet()==1)throw new IllegalStateException("native failure");return 7;});
        try {retry.get();throw new AssertionError("failure swallowed");} catch(IllegalStateException expected) {}
        if(retry.get()!=7||retry.get()!=7||attempts.get()!=2)throw new AssertionError("retry broken");
        if(MapperLoadScope.defer())throw new AssertionError("new entries deferred");
        try(var scope=MapperLoadScope.begin()) {
            if(!MapperLoadScope.defer())throw new AssertionError();
            try(var nested=MapperLoadScope.begin()) {if(!MapperLoadScope.defer())throw new AssertionError();}
            if(!MapperLoadScope.defer())throw new AssertionError("nested restore");
            var crossed=new java.util.concurrent.atomic.AtomicBoolean();
            var other=new Thread(()->crossed.set(MapperLoadScope.defer()));
            other.start();other.join();
            if(crossed.get())throw new AssertionError("cross-thread scope");
            throw new IllegalStateException("scope exit");
        } catch(IllegalStateException expected) {}
        if(MapperLoadScope.defer())throw new AssertionError("exception scope leaked");
        var pending=DeferredOpacity.class.getDeclaredField("pending");pending.setAccessible(true);
        if(pending.get(one)!=null)throw new AssertionError("native closure retained");
        System.out.println("PASS: 20,000 historical entries compute only 100 used results; exact values, concurrent once, retry, scope cleanup, closure release");
    }
}
