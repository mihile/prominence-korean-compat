package local.prominence.work;
import java.util.*;

public final class ModelWorkSliceTests {
    static int checks;
    static void check(boolean b) { if(!b)throw new AssertionError("check "+checks);checks++; }
    public static void main(String[] args)throws Exception {
        check(ModelWorkSlice.continueBatch(true,Long.MAX_VALUE));
        try(var slice=ModelWorkSlice.begin(0)){check(!ModelWorkSlice.continueBatch(true,100_000_000));}
        try(var active=InitialWork.begin()) {
            try(var slice=ModelWorkSlice.begin(0)) {
                check(ModelWorkSlice.continueBatch(true,3_999_999));
                check(!ModelWorkSlice.continueBatch(true,4_000_000));
                check(!ModelWorkSlice.continueBatch(false,0));
                try(var inner=ModelWorkSlice.begin(100_000_000)){check(ModelWorkSlice.continueBatch(true,100_000_001));}
                check(!ModelWorkSlice.continueBatch(true,9_000_000));
                Thread other=new Thread(()->check(ModelWorkSlice.continueBatch(true,Long.MAX_VALUE)));other.start();other.join();
            }
            // One complete FIFO model always runs before a yield; no removal on the boundary.
            Deque<Integer> q=new ArrayDeque<>();for(int i=0;i<100;i++)q.add(i);List<Integer> out=new ArrayList<>();int batches=0;
            while(!q.isEmpty()) {try(var slice=ModelWorkSlice.begin(0)){long time=0;boolean more;do{out.add(q.removeFirst());time+=3_000_000;more=!q.isEmpty();}while(ModelWorkSlice.continueBatch(more,time));batches++;}}
            check(batches==50);for(int i=0;i<100;i++)check(out.get(i)==i);
            try(var slice=ModelWorkSlice.begin(0)){throw new IllegalStateException("native failure");}catch(IllegalStateException expected){}
            check(ModelWorkSlice.continueBatch(true,Long.MAX_VALUE));
            var a=ModelWorkSlice.begin(0);var b=ModelWorkSlice.begin(0);try{a.close();throw new AssertionError();}catch(IllegalStateException expected){}b.close();a.close();a.close();
            try(var slice=ModelWorkSlice.begin(Long.MAX_VALUE-10)){check(ModelWorkSlice.continueBatch(true,Long.MIN_VALUE+10));}
        }
        check(!InitialWork.active());
        check(ModelWorkSlice.continueBatch(true,Long.MAX_VALUE));
        System.out.println("PASS: model slice "+checks+" checks; FIFO, progress, native idle, nested/error/thread scope, overflow; no game/GL");
    }
}
