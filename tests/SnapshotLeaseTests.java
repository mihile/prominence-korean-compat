import local.prominence.autoupdate.SnapshotLease;
import java.util.concurrent.atomic.AtomicInteger;

public final class SnapshotLeaseTests {
    public static void main(String[] args){
        var disposed=new AtomicInteger();Object texture=new Object();
        var offer=new SnapshotLease<>(texture,x->{if(x!=texture)throw new AssertionError("Wrong capture");disposed.incrementAndGet();});
        var channel=offer.retain();offer.release();
        if(disposed.get()!=0 || channel.value()!=texture)throw new AssertionError("Channel change destroyed the captured menu");
        var next=channel.retain();channel.release();next.release();
        if(disposed.get()!=1 || next.value()!=null)throw new AssertionError("Final close did not release capture exactly once");
        try{next.retain();throw new AssertionError("Closed capture reused");}catch(IllegalStateException expected){}
        try{next.release();throw new AssertionError("Double release accepted");}catch(IllegalStateException expected){}
        System.out.println("PASS popup offer/channel transfer, final release, no retained capture and invalid reuse checks");
    }
}
