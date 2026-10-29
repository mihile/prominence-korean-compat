package local.prominence.rewards;
import com.google.gson.*;import java.io.*;import java.nio.charset.StandardCharsets;import java.util.*;
public final class RewardPolicy {
 public static final RewardPolicy INSTANCE=load();
 public final double maximumBase,lootingRate,luckRate;public final Set<String> quests;
 private RewardPolicy(double max,double looting,double luck,Set<String> quests){maximumBase=max;lootingRate=looting;luckRate=luck;this.quests=Set.copyOf(quests);}
 private static RewardPolicy load(){try{
  JsonObject p;try(var in=RewardPolicy.class.getResourceAsStream("/assets/prominence_korean_compat/reward_scaling_policy.json")){p=JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonObject();}
  var ids=new HashSet<String>();try(var in=RewardPolicy.class.getResourceAsStream("/assets/prominence_korean_compat/reward_quest_items.json")){for(var id:JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonArray())ids.add(id.getAsString());}
  if(p.get("schema").getAsInt()!=1||!p.get("positiveLuckOnly").getAsBoolean()||p.get("maximumChance").getAsDouble()!=1)throw new IllegalArgumentException("Unsupported reward policy");
  for(String key:List.of("maximumBaseChance","lootingRelativeBonus","luckRelativeBonus")){double v=p.get(key).getAsDouble();if(!Double.isFinite(v)||v<0||v>1)throw new IllegalArgumentException("Invalid reward coefficient");}
  return new RewardPolicy(p.get("maximumBaseChance").getAsDouble(),p.get("lootingRelativeBonus").getAsDouble(),p.get("luckRelativeBonus").getAsDouble(),ids);
 }catch(Exception e){throw new IllegalStateException("Invalid reward scaling policy",e);}}
}
