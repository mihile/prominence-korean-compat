package local.prominence.work;
import java.util.*;

public final class StateOrderTests {
    public static void main(String[] args) {
        var states=new ArrayList<Object>();for(int i=0;i<100_000;i++)states.add(new Object());
        var blocks=List.of(states.subList(0,40_000),states.subList(40_000,100_000));
        if(!StateOrder.identical(blocks,x->x,states.size(),states::get))throw new AssertionError("unchanged order rebuilt");
        var swapped=new ArrayList<>(states);Collections.swap(swapped,600,601);
        if(StateOrder.identical(blocks,x->x,states.size(),swapped::get))throw new AssertionError("changed IDs skipped");
        swapped.set(600,new Object());
        if(StateOrder.identical(blocks,x->x,states.size(),swapped::get))throw new AssertionError("new state skipped");
        if(StateOrder.identical(blocks,x->x,states.size()-1,states::get))throw new AssertionError("length mismatch skipped");
        if(StateOrder.identical(blocks,x->x,states.size()+1,states::get))throw new AssertionError("old extra state skipped");
        var a=new String("same");var b=new String("same");
        if(StateOrder.identical(List.of(List.of(a)),x->x,1,i->b))throw new AssertionError("equals used instead of identity");
        if(!StateOrder.identical(List.<List<Object>>of(),x->x,0,i->{throw new AssertionError();}))throw new AssertionError("empty");
        System.out.println("PASS: 100000 unchanged states avoid rebuild; changed order, membership, size and equal-but-distinct states fall back");
    }
}
