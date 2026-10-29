package local.prominence.rewards;
import fzzyhmstrs.emi_loot.util.LootTablePools;import java.util.*;import net.minecraft.*;
public final class RewardRegistry {
 public record Route(String table,boolean combat,Set<String> uniqueItems){public Route{uniqueItems=Set.copyOf(uniqueItems);}}
 private static Map<class_55,Route> routes=new WeakHashMap<>();private static Set<String> ingredients=Set.of();private static long epoch;
 private static long recipesAt=-1;
 private static List<RewardDefinition> metadata;
 private RewardRegistry(){}
 public static synchronized long epoch(){return epoch;}
 public static synchronized void disable(){routes=new WeakHashMap<>();ingredients=Set.of();recipesAt=++epoch;metadata=List.of();}
 public static synchronized boolean required(String id){return ingredients.contains(id)||RewardPolicy.INSTANCE.quests.contains(id);}
 public static synchronized Route route(class_55 pool){var r=routes.get(pool);return r==null||r.table().isEmpty()?null:r;}
 public static synchronized void index(Map<class_8488<?>,?> tables){
  var next=new WeakHashMap<class_55,Route>();
  for(var e:tables.entrySet())if(e.getValue() instanceof class_52 table){
   var id=e.getKey().comp_1475();String path=id.method_12832();boolean mob=path.startsWith("entities/"),chest=path.startsWith("chests/");if(!mob&&!chest)continue;
   var pools=((LootTablePools)table).getPools();var counts=new HashMap<String,Integer>();boolean complete=true;
   for(var pool:pools){var local=new HashSet<String>();for(var entry:((fzzyhmstrs.emi_loot.mixins.LootPoolAccessor)pool).getEntries()){
    if(entry instanceof class_77){var item=((fzzyhmstrs.emi_loot.mixins.ItemEntryAccessor)entry).getItem();local.add(class_7923.field_41178.method_10221(item).toString());}
    else if(!(entry instanceof class_73))complete=false;
   }for(var item:local)counts.merge(item,1,Integer::sum);}
   var unique=new HashSet<String>();if(complete)counts.forEach((item,count)->{if(count==1)unique.add(item);});
   for(var pool:pools){
    var r=new Route(id.toString(),mob,unique);var old=next.putIfAbsent(pool,r);if(old!=null&&!old.equals(r))next.put(pool,new Route("",false,Set.of()));
   }
  }routes=next;epoch++;metadata=null;
 }
 public static synchronized List<RewardDefinition> snapshot(class_1863 recipes){
  if(metadata!=null)return metadata;
  if(recipesAt!=epoch){var found=new HashSet<String>();for(var recipe:recipes.method_8126())for(var ingredient:recipe.method_8117())for(var stack:ingredient.method_8105())found.add(class_7923.field_41178.method_10221(stack.method_7909()).toString());ingredients=Set.copyOf(found);recipesAt=++epoch;}
  var result=new ArrayList<RewardDefinition>();
  for(var pool:new ArrayList<>(routes.keySet())){
   if(pool==null)continue;var plan=((RewardPool)pool).prominence$displayPlan();if(plan!=null){var definition=plan.definition();if(definition.valid())result.add(definition);else org.slf4j.LoggerFactory.getLogger("Prominence compatibility").warn("Skipped invalid reward metadata for {}",plan.route().table());}
  }
  metadata=List.copyOf(result);
  org.slf4j.LoggerFactory.getLogger("Prominence compatibility").info("Reward index: {} scalable/native display pools; recipe/quest references cached for this reload",metadata.size());
  return metadata;
 }
}
