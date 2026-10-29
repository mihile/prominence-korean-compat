package local.prominence.work;

import fzzyhmstrs.emi_loot.client.ClientBuiltPool;
import fzzyhmstrs.emi_loot.util.ConditionalStack;
import java.util.*;
import net.minecraft.*;

public final class LootEntityDisplay {
    private LootEntityDisplay(){}
    static LootEntityRule.Node node(class_2561 text,int depth){
        if(depth>32)return new LootEntityRule.Node("unknown","",List.of(),List.of());
        var args=new ArrayList<LootEntityRule.Node>();String key="",literal="";
        if(text.method_10851() instanceof class_2588 translated){
            key=translated.method_11022();
            for(Object arg:translated.method_11023())args.add(arg instanceof class_2561 t?node(t,depth+1):new LootEntityRule.Node("",String.valueOf(arg),List.of(),List.of()));
        }else if(text.method_10855().isEmpty())literal=text.getString();
        var siblings=new ArrayList<LootEntityRule.Node>();for(var sibling:text.method_10855())siblings.add(node(sibling,depth+1));
        return new LootEntityRule.Node(key,literal,args,siblings);
    }
    public static List<ClientBuiltPool> filter(List<ClientBuiltPool> pools,class_2960 mob){
        if(!class_7923.field_41177.method_10250(mob))return pools;
        var type=class_7923.field_41177.method_10223(mob);
        if(!(type.method_5897().method_10851() instanceof class_2588 name))return pools;
        var memo=new IdentityHashMap<List<class_3545<Integer,class_2561>>,Boolean>();
        java.util.function.Predicate<List<class_3545<Integer,class_2561>>> possible=conditions->memo.computeIfAbsent(conditions,list->{
            var nodes=list.stream().map(pair->node(pair.method_15441(),0)).toList();
            // EMI's general entity-predicate text loses vehicle/passenger details. Only
            // prune the reviewed Botania beheading table, whose predicates are direct victim types.
            return LootEntityRule.mayDisplay(nodes,name.method_11022(),tag->{
                    var id=class_2960.method_12829(tag);if(id==null)return LootEntityRule.Result.UNKNOWN;
                    var key=class_6862.method_40092(class_7924.field_41266,id);
                    if(class_7923.field_41177.method_40266(key).isEmpty())return LootEntityRule.Result.UNKNOWN;
                    return type.method_20210(key)?LootEntityRule.Result.YES:LootEntityRule.Result.NO;
                });
        });
        var output=new ArrayList<ClientBuiltPool>();boolean changed=false;
        for(var pool:pools){
            if(!possible.test(pool.conditions())){changed=true;continue;}
            List<ConditionalStack> kept=pool.stacks().stream().filter(s->possible.test(s.conditions())).toList();
            if(kept.size()!=pool.stacks().size())changed=true;
            if(!kept.isEmpty())output.add(kept.size()==pool.stacks().size()?pool:new ClientBuiltPool(pool.conditions(),kept));
        }
        return changed?output:pools;
    }
}
