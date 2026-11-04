package local.prominence.work;

import java.util.*;
import net.minecraft.*;
import fzzyhmstrs.emi_loot.client.*;
import fzzyhmstrs.emi_loot.util.ConditionalStack;

/** Display-only representation of reviewed rewards granted outside loot tables. */
public final class SpecialLootDisplay {
    private SpecialLootDisplay() {}
    public record Drop(String source,String mob,String item,float chance,String note) {}
    private static final List<Drop> DROPS=List.of(
      new Drop("souls", "minecraft:wither", "soulsweapons:shard_of_uncertainty",100,"wither"),
      new Drop("souls", "minecraft:wither", "soulsweapons:lord_soul_void",100,"wither"),
      new Drop("souls", "minecraft:ender_dragon", "soulsweapons:lord_soul_purple",100,"dragon"),
      new Drop("faceless", "eldritch_end:the_faceless", "eldritch_end:xal",100,"faceless"),
      new Drop("eye", "adventurez:the_eye", "adventurez:prime_eye",1,"eye"),
      new Drop("cornelia", "aquamirae:captain_cornelia", "aquamirae:tidepiercer",20,"cornelia_normal"),
      new Drop("cornelia", "aquamirae:captain_cornelia", "aquamirae:jawbreaker",20,"cornelia_winter"),
      new Drop("shattered", "deeperdarker:shattered", "deeperdarker:shattered_head",100,"charged"),
      new Drop("skeleton", "graveyard:skeleton_creeper", "graveyard:creeper_skeleton",50,"charged"),
      new Drop("enderman", "minecraft:enderman", "supplementaries:enderman_head",100,"charged_config")
    );
    public static List<LootReceiver> addMissing(List<LootReceiver> input) {
        var output=new ArrayList<LootReceiver>(input);var known=new HashSet<class_2960>();
        for(var entry:input)if(entry instanceof ClientMobLootTable t)known.add(t.mobId);
        for(var entry:DROPS) {
            var mob=new class_2960(entry.mob());var item=new class_2960(entry.item());
            if(!DirectLootGuard.allowed(entry.source())||!class_7923.field_41177.method_10250(mob)||!class_7923.field_41178.method_10250(item)||!known.add(mob))continue;
            output.add(new ClientMobLootTable(new class_2960("prominence_korean_compat","entities/direct_"+mob.method_12836()+"_"+mob.method_12832()),mob,Map.of()));
        }
        return output;
    }
    public static List<ClientBuiltPool> append(List<ClientBuiltPool> input,class_2960 mob) {
        var out=new ArrayList<ClientBuiltPool>(input);boolean changed=false;
        for(var drop:DROPS){
            if(!drop.mob().equals(mob.toString())||!DirectLootGuard.allowed(drop.source()))continue;
            var id=new class_2960(drop.item());if(!class_7923.field_41178.method_10250(id))continue;
            var item=class_7923.field_41178.method_10223(id);
            if(out.stream().flatMap(p->p.stacks().stream()).flatMap(s->s.getRawStacks().stream()).anyMatch(s->s.method_7909()==item))continue;
            var note=class_2561.method_43471("prominence_korean_compat.direct_loot."+drop.note());
            var row=new ConditionalStack(List.of(new class_3545<Integer,class_2561>(0,note)),drop.chance(),List.of(new class_1799(item)));
            out.add(new ClientBuiltPool(List.of(),List.of(row)));changed=true;
        }
        return changed?out:input;
    }
    public static Double observedChance(String item,double expected,List<class_2561> conditions){
        for(var drop:DROPS)if(drop.item().equals(item)&&Math.abs(drop.chance()-expected)<.0001&&DirectLootGuard.allowed(drop.source())&&conditions.stream().anyMatch(t->t.method_10851() instanceof class_2588 tr && tr.method_11022().equals("prominence_korean_compat.direct_loot."+drop.note())))return (double)drop.chance();
        return null;
    }
}
