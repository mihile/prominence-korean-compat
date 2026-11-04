package local.prominence.work;

import java.util.*;
import net.minecraft.*;
import fzzyhmstrs.emi_loot.client.*;
import fzzyhmstrs.emi_loot.util.ConditionalStack;

/** Display-only representation of rewards that their mods grant in entity code. */
public final class SpecialLootDisplay {
    private SpecialLootDisplay() {}
    private static final Map<String,String> ITEMS=Map.of("minecraft:wither","soulsweapons:shard_of_uncertainty", "eldritch_end:the_faceless","eldritch_end:xal");
    public static List<LootReceiver> addMissing(List<LootReceiver> input) {
        var output=new ArrayList<LootReceiver>(input);
        for(var entry:ITEMS.entrySet()) {
            var mob=new class_2960(entry.getKey()); var item=new class_2960(entry.getValue());
            if(!class_7923.field_41177.method_10250(mob)||!class_7923.field_41178.method_10250(item))continue;
            boolean exists=input.stream().anyMatch(r->r instanceof ClientMobLootTable t && mob.equals(t.mobId));
            if(!exists)output.add(new ClientMobLootTable(new class_2960("prominence_korean_compat", "entities/direct_"+mob.method_12832()),mob,Map.of()));
        }
        return output;
    }
    public static List<ClientBuiltPool> append(List<ClientBuiltPool> input,class_2960 mob) {
        var id=ITEMS.get(mob.toString());if(id==null)return input;
        var itemId=new class_2960(id);if(!class_7923.field_41178.method_10250(itemId))return input;
        var item=class_7923.field_41178.method_10223(itemId);
        if(input.stream().flatMap(p->p.stacks().stream()).flatMap(s->s.getRawStacks().stream()).anyMatch(s->s.method_7909()==item))return input;
        String key=mob.toString().equals("minecraft:wither")?"prominence_korean_compat.direct_loot.wither":"prominence_korean_compat.direct_loot.faceless";
        var conditions=List.of(new class_3545<Integer,class_2561>(0,class_2561.method_43471(key)));
        var row=new ConditionalStack(conditions,100.0f,List.of(new class_1799(item)));
        var out=new ArrayList<ClientBuiltPool>(input);out.add(new ClientBuiltPool(List.of(),List.of(row)));return out;
    }
}
