package local.prominence.work;
import java.util.*;import java.nio.file.*;import com.google.gson.*;
public final class LootUiTests {
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
