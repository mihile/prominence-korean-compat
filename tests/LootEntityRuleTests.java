package local.prominence.work;

import java.util.*;
import java.util.function.Function;
import com.google.gson.*;
import net.minecraft.class_2561;

public final class LootEntityRuleTests {
    static int checks;
    static LootEntityRule.Node node(String key,LootEntityRule.Node... args){return new LootEntityRule.Node(key,"",List.of(args),List.of());}
    static LootEntityRule.Node literal(String text){return new LootEntityRule.Node("",text,List.of(),List.of());}
    static LootEntityRule.Node victim(String type){return node("emi_loot.condition.entity_props",node("emi_loot.entity_predicate.entity_this",type.startsWith("#")?node("emi_loot.entity_predicate.type_tag",literal(type)):node("emi_loot.entity_predicate.type_single",node("entity."+type.replace(':','.')))));}
    static final LootEntityRule.Node AXE=node("emi_loot.condition.entity_props",node("emi_loot.entity_predicate.entity_killer",node("emi_loot.item_predicate.items",node("item.botania.elementium_axe"))));
    static final Function<String,LootEntityRule.Result> UNKNOWN=tag->LootEntityRule.Result.UNKNOWN;
    static void check(boolean good,String why){checks++;if(!good)throw new AssertionError(why);}
    static LootEntityRule.Node condition(JsonObject c){
        String type=c.get("condition").getAsString().replace("minecraft:","");
        if(type.equals("entity_properties")){
            if(!c.get("entity").getAsString().equals("this"))return AXE;
            return victim(c.getAsJsonObject("predicate").get("type").getAsString());
        }
        if(type.equals("any_of")){var terms=new ArrayList<LootEntityRule.Node>();for(var term:c.getAsJsonArray("terms"))terms.add(condition(term.getAsJsonObject()));return new LootEntityRule.Node("emi_loot.condition.any_of_2","",terms,List.of());}
        if(type.equals("inverted"))return node("emi_loot.condition.invert",condition(c.getAsJsonObject("term")));
        return node("emi_loot.condition.chance_looting");
    }
    public static void main(String[] args)throws Exception {
        check(!LootEntityRule.mayDisplay(List.of(AXE,victim("minecraft:piglin")),"entity.adventurez.blackstone_golem",UNKNOWN),"Golem lists piglin head");
        check(LootEntityRule.mayDisplay(List.of(AXE,victim("minecraft:piglin")),"entity.minecraft.piglin",UNKNOWN),"Actual piglin lost head route");
        check(LootEntityRule.mayDisplay(List.of(victim("minecraft:piglin")),"entity.adventurez.blackstone_golem",UNKNOWN),"Unreviewed table was filtered");
        var either=node("emi_loot.condition.any_of_2",victim("minecraft:piglin"),victim("minecraft:piglin_brute"));
        check(!LootEntityRule.mayDisplay(List.of(AXE,either),"entity.adventurez.blackstone_golem",UNKNOWN),"OR mismatch not rejected");
        check(LootEntityRule.mayDisplay(List.of(AXE,either),"entity.minecraft.piglin_brute",UNKNOWN),"OR match rejected");
        var chain=new LootEntityRule.Node("","",List.of(),List.of(node("emi_loot.condition.any_of_3",victim("minecraft:zombie")),node("emi_loot.condition.any_of_3a",victim("minecraft:husk")),node("emi_loot.condition.any_of",victim("minecraft:drowned"))));
        check(!LootEntityRule.mayDisplay(List.of(AXE,chain),"entity.adventurez.blackstone_golem",UNKNOWN),"Native OR sibling chain not rejected");
        check(LootEntityRule.mayDisplay(List.of(AXE,chain),"entity.minecraft.husk",UNKNOWN),"Native OR sibling chain lost valid husk");
        check(LootEntityRule.mayDisplay(List.of(AXE,node("emi_loot.condition.invert",victim("minecraft:zombie"))),"entity.minecraft.zombie",UNKNOWN),"Incomplete inverse inferred as impossible");
        check(LootEntityRule.mayDisplay(List.of(AXE,victim("#minecraft:skeletons")),"entity.adventurez.blackstone_golem",UNKNOWN),"Missing tag treated as an empty known tag");
        check(!LootEntityRule.mayDisplay(List.of(AXE,victim("#minecraft:skeletons")),"entity.adventurez.blackstone_golem",tag->LootEntityRule.Result.NO),"Known tag mismatch retained");
        var actual=class_2561.method_43469("emi_loot.condition.entity_props",class_2561.method_43469("emi_loot.entity_predicate.entity_this",class_2561.method_43469("emi_loot.entity_predicate.type_single",class_2561.method_43471("entity.minecraft.piglin"))));
        check(LootEntityRule.evaluate(LootEntityDisplay.node(actual,0),"entity.adventurez.blackstone_golem",UNKNOWN)==LootEntityRule.Result.NO,"Native Text translation AST not read");
        try(var stream=LootEntityRuleTests.class.getResourceAsStream("/data/botania/loot_tables/elementium_axe_beheading.json")){
            var table=JsonParser.parseReader(new java.io.InputStreamReader(stream,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();int rejected=0;
            for(var pool:table.getAsJsonArray("pools")){
                var conditions=new ArrayList<LootEntityRule.Node>();for(var c:pool.getAsJsonObject().getAsJsonArray("conditions"))conditions.add(condition(c.getAsJsonObject()));
                if(!LootEntityRule.mayDisplay(conditions,"entity.adventurez.blackstone_golem",tag->LootEntityRule.Result.NO))rejected++;
            }
            check(rejected==7,"Actual installed Botania beheading pools not all filtered from golem");
        }
        try(var stream=LootEntityRuleTests.class.getResourceAsStream("/data/adventurez/loot_tables/entities/blackstone_golem.json")){
            var table=JsonParser.parseReader(new java.io.InputStreamReader(stream,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
            for(var p:table.getAsJsonArray("pools"))for(var e:p.getAsJsonObject().getAsJsonArray("entries"))if(e.getAsJsonObject().get("name").getAsString().equals("adventurez:blackstone_golem_heart")){
                var c=p.getAsJsonObject().getAsJsonArray("conditions").get(1).getAsJsonObject();check(Math.abs(c.get("chance").getAsDouble()-.05)<1e-8&&Math.abs(c.get("looting_multiplier").getAsDouble()-.03)<1e-8,"Installed heart drop probability changed");
            }
        }
        System.out.println("PASS "+checks+" victim/OR/tag/unknown/native Text/real Botania table and AdventureZ probability checks; no world or GL");
    }
}
