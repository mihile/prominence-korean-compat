package local.prominence.rewards.client;
import local.prominence.rewards.*;import java.util.*;import net.minecraft.*;
public final class RewardClientState {
 private static volatile Map<String,List<RewardDefinition>> tables=Map.of();
 public static void clear(){tables=Map.of();}
 public static boolean active(RewardDefinition d){return tables.getOrDefault(d.table(),List.of()).contains(d);}
 public static void install(List<RewardDefinition> definitions){var map=new HashMap<String,List<RewardDefinition>>();for(var d:definitions)map.computeIfAbsent(d.table(),k->new ArrayList<>()).add(d);var copy=new HashMap<String,List<RewardDefinition>>();map.forEach((k,v)->copy.put(k,List.copyOf(v)));tables=Map.copyOf(copy);}
 public static RewardDefinition find(String table,List<String> items,double expected,List<class_2561> conditions){
  RewardDefinition result=null;
  for(var d:tables.getOrDefault(table,List.of())){
   if(items.isEmpty()||!items.stream().allMatch(id->d.outputs().stream().anyMatch(o->o.item().equals(id)&&(d.weighted()&&o.unique()||Math.abs(o.expected()-expected)<.001))))continue;
   // EMI may combine items in one cycling slot. A shared tooltip is valid only for equal rates.
   var selected=d.outputs().stream().filter(o->items.contains(o.item())).toList();
   if(selected.stream().anyMatch(o->Math.abs(o.share()-selected.get(0).share())>1e-10 || d.weighted()&&!d.boosts().get(o.index()).equals(d.boosts().get(selected.get(0).index()))))continue;
   if(d.combat()&&d.kind().equals("chance")&&conditions.stream().noneMatch(t->matchesGate(t,d.base())))continue;
   if(result!=null)return null;result=d;
  }return result;
 }
 public static boolean matchesGate(class_2561 text,double base){if(!(text.method_10851() instanceof class_2588 c)||!c.method_11022().equals("emi_loot.condition.chance")||!text.method_10855().isEmpty())return false;var a=c.method_11023();if(a.length!=1)return false;try{return Math.abs(Double.parseDouble(a[0] instanceof class_2561 t?t.getString():a[0].toString())/100-base)<.000001;}catch(Exception e){return false;}}
}
