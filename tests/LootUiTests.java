package local.prominence.work;
import java.util.*;import java.nio.file.*;import com.google.gson.*;
public final class LootUiTests {
 static net.minecraft.class_2561 text(String key,Object... args){return net.minecraft.class_2561.method_43469(key,args);}
 static net.minecraft.class_2561 literal(String value){return net.minecraft.class_2561.method_43470(value);}
 // Mirror native EMI Loot's emitted Text shape, using the actual installed JSON below.
 static net.minecraft.class_2561 condition(JsonObject c){
  String type=c.get("condition").getAsString().replace("minecraft:","");
  if(type.equals("entity_properties")){
   boolean self=c.get("entity").getAsString().equals("this");net.minecraft.class_2561 predicate;
   if(self){String name=c.getAsJsonObject("predicate").get("type").getAsString();predicate=name.startsWith("#")?text("emi_loot.entity_predicate.type_tag",literal(name)):text("emi_loot.entity_predicate.type_single",text("entity."+name.replace(':','.')));}
   else predicate=text("emi_loot.item_predicate.items",text("item.botania.elementium_axe"));
   return text("emi_loot.condition.entity_props",text("emi_loot.entity_predicate.entity_"+(self?"this":"killer"),text("emi_loot.entity_predicate.base",predicate)));
  }
  if(type.equals("any_of")){
   var terms=new ArrayList<net.minecraft.class_2561>();for(var t:c.getAsJsonArray("terms"))terms.add(condition(t.getAsJsonObject()));
   if(terms.size()==2)return text("emi_loot.condition.any_of_2",terms.get(0),terms.get(1));
   var chain=net.minecraft.class_2561.method_43473();for(int i=0;i<terms.size();i++)chain.method_10852(text(i==terms.size()-1?"emi_loot.condition.any_of":i==terms.size()-2?"emi_loot.condition.any_of_3a":"emi_loot.condition.any_of_3",terms.get(i)));return chain;
  }
  if(type.equals("inverted"))return text("emi_loot.condition.invert",condition(c.getAsJsonObject("term")));
  if(type.equals("random_chance_with_looting"))return text("emi_loot.condition.chance_looting",literal(Float.toString(c.get("chance").getAsFloat()*100)),literal(Float.toString(c.get("looting_multiplier").getAsFloat()*100)));
  return text("unknown.condition");
 }
 public static void main(String[] args)throws Exception {
  for(float f:new float[]{0.000001f,0.0001f,0.001f,0.01f,0.05f,0.1f,1f,1.5f}){String s=LootNumbers.precise(f);if(Double.parseDouble(s)<=0)throw new AssertionError("positive rounded to zero "+f);}
  if(!LootNumbers.percent(7.6899999).equals("7.68")||!LootNumbers.percent(7.6999996).equals("7.69")||!LootNumbers.percent(1.239).equals("1.23"))throw new AssertionError("percent truncation");
  if(!LootNumbers.percent(.0001).equals("<0.01")||!LootNumbers.percent(0).equals("0")||!LootNumbers.percent(100).equals("100")||!LootNumbers.percent(Double.NaN).equals("?"))throw new AssertionError("rare and invalid probabilities");
  if(LootDropTooltip.hit("minecraft:entities/piglin_brute","adventurez:gilded_blackstone_shard",10)!=10||LootDropTooltip.hit("minecraft:entities/piglin_brute","adventurez:gilded_blackstone_shard",20)!=null)throw new AssertionError("injected binomial and server-value mismatch");
  if(LootDropTooltip.chestHit("minecraft:chests/bastion_bridge","adventurez:gilded_blackstone_shard",1)!=1||LootDropTooltip.hit("minecraft:gameplay/piglin_bartering","adventurez:gilded_blackstone_shard",1)!=1)throw new AssertionError("all injected shard routes");
  if(LootDropTooltip.hit("adventurez:entities/blackstone_golem","minecraft:ancient_debris",100)!=75)throw new AssertionError("two half-chance draws must be 75%, not 100%");
  if(!LootDropTooltip.table("emi_loot:/mob/minecraft/entities/piglin_brute").equals("minecraft:entities/piglin_brute"))throw new AssertionError("recipe table");
  if(!LootDropTooltip.shortBeheading("가이아의 수호자",true).equals("엘레멘티움 도끼로 가이아의 수호자 처치"))throw new AssertionError("concise required weapon/victim");
  var axe=net.minecraft.class_2561.method_43469("emi_loot.condition.entity_props",net.minecraft.class_2561.method_43469("emi_loot.entity_predicate.entity_killer",net.minecraft.class_2561.method_43471("item.botania.elementium_axe")));
  var victim=net.minecraft.class_2561.method_43469("emi_loot.condition.entity_props",net.minecraft.class_2561.method_43469("emi_loot.entity_predicate.entity_this",net.minecraft.class_2561.method_43469("emi_loot.entity_predicate.type_single",net.minecraft.class_2561.method_43471("entity.botania.doppleganger"))));
  var chance=net.minecraft.class_2561.method_43469("emi_loot.condition.chance_looting",net.minecraft.class_2561.method_43470("7.6899996"),net.minecraft.class_2561.method_43470("7.6899996"));
  var head=LootDropTooltip.lines("emi_loot:/mob/minecraft/entities/piglin_brute",List.of("botania:gaia_head"),100,List.of(axe,victim,chance),true,key->"가이아의 수호자").stream().map(net.minecraft.class_2561::getString).toList();
  if(!head.equals(List.of("드롭 확률: 7.68% · 약탈 레벨당 +7.68%p","엘레멘티움 도끼로 가이아의 수호자 처치")))throw new AssertionError("actual head summary "+head);
  var nativeHead=new local.prominence.rewards.client.RewardClientText.TooltipSource("minecraft:entities/piglin_brute",List.of("botania:gaia_head"),100,List.of(axe,victim,chance),head.stream().map(LootUiTests::literal).toList(),List.of(axe,victim));
  var currentHead=LootDropTooltip.currentNative(nativeHead,1,10,true,true).stream().map(net.minecraft.class_2561::getString).toList();
  if(!currentHead.get(0).equals("드롭 확률: 15.37%")||!currentHead.get(currentHead.size()-1).equals("엘레멘티움 도끼로 가이아의 수호자 처치")||currentHead.size()!=4)throw new AssertionError("native current Looting with compact condition "+currentHead);
  var invalidChance=text("emi_loot.condition.chance",literal("NaN"));
  var invalid=new local.prominence.rewards.client.RewardClientText.TooltipSource("minecraft:entities/piglin_brute",List.of("adventurez:gilded_blackstone_shard"),10,List.of(invalidChance),List.of(literal("unknown")),List.of());
  if(!LootDropTooltip.currentNative(invalid,3,0,true,true).get(0).getString().equals("unknown"))throw new AssertionError("invalid native chance must preserve fallback");
  var chestSource=new local.prominence.rewards.client.RewardClientText.TooltipSource("minecraft:chests/bastion_bridge",List.of("adventurez:gilded_blackstone_shard"),1,List.of(),List.of(literal("old")),List.of());
  if(!LootDropTooltip.currentNative(chestSource,10,10,true,false).get(0).getString().equals("드롭 확률: 1%"))throw new AssertionError("native chest probability not mob chance or Looting-scaled");
  var either=text("emi_loot.condition.any_of_2",condition(JsonParser.parseString("{\"condition\":\"entity_properties\",\"entity\":\"this\",\"predicate\":{\"type\":\"minecraft:piglin\"}}").getAsJsonObject()),condition(JsonParser.parseString("{\"condition\":\"entity_properties\",\"entity\":\"this\",\"predicate\":{\"type\":\"minecraft:piglin_brute\"}}").getAsJsonObject()));
  java.util.function.Function<String,String> names=k->Map.of("entity.minecraft.piglin","피글린","entity.minecraft.piglin_brute","난폭한 피글린","entity.minecraft.zombie","좀비","entity.minecraft.husk","허스크","entity.minecraft.drowned","드라운드").getOrDefault(k,k);
  var piglin=LootDropTooltip.lines("emi_loot:/mob/minecraft/entities/piglin_brute",List.of("minecraft:piglin_head"),100,List.of(axe,either,chance),true,names).stream().map(net.minecraft.class_2561::getString).toList();
  if(!piglin.equals(List.of("드롭 확률: 7.68% · 약탈 레벨당 +7.68%p","엘레멘티움 도끼로 피글린 또는 난폭한 피글린 처치")))throw new AssertionError("reported OR duplication "+piglin);
  var extra=either.method_27661().method_10852(literal("추가 조건 보존"));
  var guarded=LootDropTooltip.lines("emi_loot:/mob/minecraft/entities/piglin_brute",List.of("minecraft:piglin_head"),100,List.of(axe,extra,chance),true,names).stream().map(net.minecraft.class_2561::getString).toList();
  if(guarded.stream().noneMatch(s->s.contains("추가 조건 보존"))||guarded.contains("엘레멘티움 도끼로 피글린 처치"))throw new AssertionError("partly understood condition was consumed "+guarded);
  try(var in=LootUiTests.class.getResourceAsStream("/data/botania/loot_tables/elementium_axe_beheading.json")){
   var table=JsonParser.parseReader(new java.io.InputStreamReader(in,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();int reviewed=0;
   for(var pool:table.getAsJsonArray("pools")){
    var p=pool.getAsJsonObject();var cs=new ArrayList<net.minecraft.class_2561>();for(var c:p.getAsJsonArray("conditions"))cs.add(condition(c.getAsJsonObject()));
    String item=p.getAsJsonArray("entries").get(0).getAsJsonObject().get("name").getAsString();
    var output=LootDropTooltip.lines("emi_loot:/mob/minecraft/entities/piglin_brute",List.of(item),100,cs,true,names).stream().map(net.minecraft.class_2561::getString).toList();
    if(output.size()!=2||!output.get(1).startsWith("엘레멘티움 도끼로 ")||output.toString().contains("emi_loot."))throw new AssertionError("native multi-target condition left over "+item+output);
    if(item.equals("minecraft:zombie_head")&&(!output.get(1).contains("좀비")||!output.get(1).contains("허스크")||!output.get(1).contains("드라운드")))throw new AssertionError("three-way target list narrowed "+output);
    if(item.equals("minecraft:skeleton_skull")&&(!output.get(1).contains("스켈레톤 계열")||!output.get(1).contains("entity.minecraft.wither_skeleton 제외")))throw new AssertionError("excluded victim lost "+output);
    reviewed++;
   }if(reviewed!=7)throw new AssertionError("beheading table coverage "+reviewed);
  }
  var gold=LootDropTooltip.lines("emi_loot:/mob/minecraft/entities/piglin_brute",List.of("adventurez:gilded_blackstone_shard"),10,List.of(),true,key->key);
  if(gold.size()!=1||!gold.get(0).getString().equals("드롭 확률: 10%"))throw new AssertionError("actual binomial summary");
  var unknown=LootDropTooltip.lines("emi_loot:/mob/custom/entities/unknown",List.of("custom:item"),200,List.of(),true,key->key);
  if(!unknown.get(0).getString().equals("드롭 확률: 확인 불가"))throw new AssertionError("mean is not a probability");
  String a=SearchDimensions.append("진홍빛 숲","minecraft:crimson_forest",List.of("minecraft:the_nether"),x->"네더");if(!a.contains("네더")||!a.contains("crimson forest"))throw new AssertionError("biome dimension/English search");
  String b=SearchDimensions.append("주조소","dungeons_arise:foundry",List.of("minecraft:overworld","prominent:past"),x->x.equals("prominent:past")?"과거":"오버월드");if(b.contains("네더")||!b.contains("과거"))throw new AssertionError("dimension guessed from name");
  String e=SearchDimensions.append("엔드 도시","minecraft:end_city",List.of("minecraft:the_end"),x->"엔드");if(!e.contains("엔더"))throw new AssertionError("End alias");
  var file=LootUiTests.class.getResourceAsStream("/assets/prominence_korean_compat/loot_title_names.json");var plan=JsonParser.parseReader(new java.io.InputStreamReader(file,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();if(plan.size()!=611)throw new AssertionError("coverage");
  var foundry=plan.getAsJsonObject("dungeons_arise:chests/foundry/foundry_lava_pit");if(!foundry.get("structure").getAsString().equals("dungeons_arise:foundry")||!foundry.get("detail").getAsString().equals("용암 구덩이"))throw new AssertionError("shared compass mapping");
  for(var item:plan.entrySet()){var row=item.getValue().getAsJsonObject();if(row.get("fallback").getAsString().isBlank())throw new AssertionError(item.getKey());}
  System.out.println("PASS: truncated/rare probabilities, real binomial routes, repeat-draw probability, stale-value fallback, concise beheading, aliases and611 loot title plans");
 }
}
