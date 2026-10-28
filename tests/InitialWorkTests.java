package local.prominence.work;

import java.util.concurrent.CountDownLatch;

public final class InitialWorkTests {
    private static int checks;
    private static void check(boolean ok,String msg){if(!ok)throw new AssertionError(msg);checks++;}
    public static void main(String[]args)throws Exception {
        check(InitialWork.budget(10,10,10,10)==10,"native scheduling outside EMI");
        int priority=Thread.currentThread().getPriority();
        InitialWork.ensureRenderThreadPriority();check(Thread.currentThread().getPriority()==priority,"render priority changed");
        var outer=InitialWork.begin();
        try {
            for(int threads=1;threads<=32;threads++)for(int busy=0;busy<=threads;busy++)for(int queued=0;queued<=threads;queued++) {
                int original=Math.max(0,threads-queued),out=InitialWork.budget(original,threads,busy,queued);
                check(out>=0&&out<=original,"never add or produce negative budget");
                if(threads<4)check(out==original,"small pool unchanged");
                else check(out==Math.max(0,Math.min(original,Math.max(2,threads/2)-busy-queued)),"respect all running and queued work");
            }
            var inner=InitialWork.begin();inner.close();inner.close();
            check(InitialWork.budget(10,10,0,0)==5,"nested close preserves outer scope");
            CountDownLatch ready=new CountDownLatch(1),finish=new CountDownLatch(1);
            Thread t=new Thread(()->{try(var scope=InitialWork.begin()){ready.countDown();try{finish.await();}catch(InterruptedException x){throw new RuntimeException(x);}}});t.start();ready.await();outer.close();
            check(InitialWork.budget(10,10,5,0)==0,"cross-thread scope remains active");finish.countDown();t.join();
            check(InitialWork.budget(10,10,5,0)==10,"final scope restores native");
            try(var failed=InitialWork.begin()){throw new IllegalStateException("fixture");}catch(IllegalStateException expected){}
            check(InitialWork.budget(10,10,5,0)==10,"failure restores native");
        }finally{outer.close();}
        System.out.println("PASS: initial EMI/terrain budget checks="+checks+"; no world or worker pool started");
    }
}
