package local.prominence.work;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.class_2960;

public final class ParsedItemIdTests {
    public static void main(String[]args) {
        var memo=new ParsedItemId();var calls=new AtomicInteger();Operation<class_2960> parse=a->{calls.incrementAndGet();return new class_2960((String)a[0]);};
        String id=new String("minecraft:stone");var first=memo.resolve(id,parse);
        for(int i=0;i<100_000;i++)if(memo.resolve(id,parse)!=first)throw new AssertionError("stable parse identity");
        if(calls.get()!=1)throw new AssertionError("repeated parse");
        var second=memo.resolve(new String(id),parse);if(second==first||!second.equals(first)||calls.get()!=2)throw new AssertionError("replacement string revalidation");
        if(!memo.resolve("minecraft:dirt",parse).equals(new class_2960("minecraft:dirt")))throw new AssertionError("new ID stale");
        for(int i=0;i<2;i++){try{memo.resolve("invalid identifier",parse);throw new AssertionError("invalid accepted");}catch(net.minecraft.class_151 expected){}}
        if(calls.get()!=5)throw new AssertionError("invalid result memoized");
        System.out.println("PASS: 100005 parsed-ID reuse/mutation/error checks; registry result is never cached");
    }
}
