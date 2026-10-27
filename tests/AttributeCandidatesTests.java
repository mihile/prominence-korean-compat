package local.prominence.work;

import elocindev.customitemattributes.api.ItemProperty;
import java.util.*;
import java.util.concurrent.*;
import net.minecraft.class_2960;

public final class AttributeCandidatesTests {
    static int checks;
    static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    static ItemProperty p(String id){return new ItemProperty(id,new ArrayList<>(List.of("mainhand")),new ArrayList<>());}
    public static void main(String[]args)throws Exception{
        var id=new class_2960("minecraft:stone");ItemProperty first=p("stone"),second=p("minecraft:stone"),other=p("minecraft:dirt");
        var source=new ArrayList<>(List.of(first,other,second));
        check(AttributeCandidates.select(source,id)==null&&!AttributeCandidates.active(),"outside search changed");
        try(var scope=AttributeCandidates.begin()){
            check(AttributeCandidates.select(source,id).equals(List.of(first,second)),"normalization/order/duplicates changed");
            first.unbreakable=true;first.affected_slots.add("head");
            check(AttributeCandidates.select(source,id).get(0)==first&&scope.rebuilds()==1,"attribute/slot values copied or needlessly cached");
            second.item="minecraft:dirt";check(AttributeCandidates.select(source,id).equals(List.of(first)),"mutable ID not reflected");
            source.set(0,other);source.set(1,first);check(AttributeCandidates.select(source,id).equals(List.of(first)),"in-place reorder not reflected");
            source.add(p("stone"));check(AttributeCandidates.select(source,id).size()==2,"list growth not reflected");
            source.get(0).item="INVALID: ID";check(AttributeCandidates.select(source,id)==null,"malformed ID error ordering bypassed");
            check(AttributeCandidates.select(source,()->{throw new AssertionError("input read before malformed-definition fallback");})==null,"malformed input fallback reordered");
            source.get(0).item="dirt";check(AttributeCandidates.select(source,id).size()==2,"corrected ID not reflected");
            source.set(0,null);check(AttributeCandidates.select(source,id)==null,"null entry bypassed");source.set(0,p("dirt"));
            source.add(new ItemProperty("stone",List.of(),List.of()){});check(AttributeCandidates.select(source,id)==null,"custom property semantics bypassed");source.remove(source.size()-1);
            check(AttributeCandidates.select(new LinkedList<>(source),id)==null,"unknown iterator semantics bypassed");
            try(var nested=AttributeCandidates.begin()){check(AttributeCandidates.select(source,id).size()==2,"nested scope failed");}
            check(AttributeCandidates.select(source,id).size()==2,"outer scope not restored");
            var executor=Executors.newSingleThreadExecutor();try{check(executor.submit(()->AttributeCandidates.select(source,id)==null).get(),"scope crossed threads");}finally{executor.shutdown();}
        }
        check(!AttributeCandidates.active()&&AttributeCandidates.select(source,id)==null,"scope retained after close");
        var many=new ArrayList<ItemProperty>();for(int i=0;i<1000;i++)many.add(p("example:item_"+i));
        try(var scope=AttributeCandidates.begin()){
            for(int i=0;i<6000;i++)check(AttributeCandidates.select(many,new class_2960("example:item_10")).equals(List.of(many.get(10))),"candidate changed");
            check(scope.originalEntries()==6_000_000&&scope.selectedEntries()==6000&&scope.rebuilds()==1,"operation reduction not observed");
        }
        System.out.println("PASS: "+checks+" attribute candidate normalization/order/mutation/error/fallback/nesting/thread/lifetime checks; fixture visits6000000->6000, not a game benchmark");
    }
}
