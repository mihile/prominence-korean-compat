package local.prominence.work;
import java.util.*;import java.nio.file.*;import com.google.gson.*;
public final class LootUiTests {
 public static void main(String[] args)throws Exception {
  for(float f:new float[]{0.000001f,0.0001f,0.001f,0.01f,0.05f,0.1f,1f,1.5f}){String s=LootNumbers.precise(f);if(Double.parseDouble(s)<=0)throw new AssertionError("positive rounded to zero "+f);}
  if(!LootNumbers.rolls(.00001,true).contains("0.001%"))throw new AssertionError("tiny percent");
  if(!LootNumbers.rolls(1,true).contains("100%"))throw new AssertionError("one roll");
  if(!LootNumbers.rolls(1.5,true).contains("150%")||!LootNumbers.rolls(1.5,true).contains("1.5회"))throw new AssertionError("expected yield clamped/misrepresented");
  if(!LootNumbers.rolls(1e-12,true).contains("<0.000001%"))throw new AssertionError("tiny positive shown as zero");
  if(!LootNumbers.rolls(0,true).contains("0%")||LootNumbers.rolls(Double.NaN,true).contains("NaN"))throw new AssertionError("zero/invalid");
  String a=SearchDimensions.append("진홍빛 숲","minecraft:crimson_forest",List.of("minecraft:the_nether"),x->"네더");if(!a.contains("네더")||!a.contains("crimson forest"))throw new AssertionError("biome dimension/English search");
  String b=SearchDimensions.append("주조소","dungeons_arise:foundry",List.of("minecraft:overworld","prominent:past"),x->x.equals("prominent:past")?"과거":"오버월드");if(b.contains("네더")||!b.contains("과거"))throw new AssertionError("dimension guessed from name");
  String e=SearchDimensions.append("엔드 도시","minecraft:end_city",List.of("minecraft:the_end"),x->"엔드");if(!e.contains("엔더"))throw new AssertionError("End alias");
  var file=LootUiTests.class.getResourceAsStream("/assets/prominence_korean_compat/loot_title_names.json");var plan=JsonParser.parseReader(new java.io.InputStreamReader(file,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();if(plan.size()!=611)throw new AssertionError("coverage");
  var foundry=plan.getAsJsonObject("dungeons_arise:chests/foundry/foundry_lava_pit");if(!foundry.get("structure").getAsString().equals("dungeons_arise:foundry")||!foundry.get("detail").getAsString().equals("용암 구덩이"))throw new AssertionError("shared compass mapping");
  for(var item:plan.entrySet()){var row=item.getValue().getAsJsonObject();if(row.get("fallback").getAsString().isBlank())throw new AssertionError(item.getKey());}
  System.out.println("PASS: rare positive values, unclamped expectations, non-probability labels, dimension/English aliases and611 loot title plans");
 }
}
