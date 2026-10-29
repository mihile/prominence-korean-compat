package local.prominence.rewards;
import fzzyhmstrs.emi_loot.mixins.*;
import java.util.*;import net.minecraft.*;

/** Live native loot data. Unknown structures or existing luck/enchantment scaling stay native. */
public record RewardPlan(RewardRegistry.Route route,Object gate,String kind,double base,List<Item> items,List<Object> leaves,List<Integer> weights,List<Boolean> boosts,List<Double> draws,boolean eligible){
 public record Item(String id,int weight,boolean valuable,boolean edible){}
 public RewardPlan {items=List.copyOf(items);leaves=List.copyOf(leaves);weights=List.copyOf(weights);boosts=List.copyOf(boosts);draws=List.copyOf(draws);}
 public boolean combat(){return route.combat();}
 public boolean weighted(){return kind.equals("weighted");}
 public RewardDefinition definition(){
  long total=weights.stream().mapToLong(Integer::longValue).sum();var out=new ArrayList<RewardDefinition.Output>();double mean=0;for(int i=0;i<draws.size();i++)mean+=i*draws.get(i);
  for(int i=0;i<items.size();i++){var item=items.get(i);if(item.id().equals("minecraft:air"))continue;double share=(double)item.weight()/total;double expected=100*share*(weighted()?mean:((kind.equals("binomial")||!combat())?base:1));out.add(new RewardDefinition.Output(item.id(),share,expected,i,route.uniqueItems().contains(item.id())));}
  var p=RewardPolicy.INSTANCE;return new RewardDefinition(route.table(),kind,base,eligible()&&combat()?p.lootingRate:0,eligible()?p.luckRate:0,combat(),out,weighted()?weights:List.of(),weighted()?boosts:List.of(),weighted()?draws:List.of());
 }
 public static Double constant(class_5658 n){return n instanceof class_44?(double)((ConstantLootNumberProviderAccessor)n).getValue():null;}
 private static Double minimum(class_5658 n){var c=constant(n);if(c!=null)return c;if(n instanceof class_5662)return constant(((UniformLootNumberProviderAccessor)n).getMin());return null;}
 private static String functionId(class_117 f){var id=class_7923.field_41134.method_10221(f.method_29321());return id==null?"":id.toString();}
 private static boolean functions(class_117[] functions){
  for(var f:functions){String id=functionId(f);
   if(!Set.of("minecraft:set_count","minecraft:set_nbt","minecraft:set_name","minecraft:set_lore","minecraft:set_attributes","minecraft:set_damage","minecraft:enchant_randomly","minecraft:enchant_with_levels","minecraft:fill_player_head").contains(id))return false;
   if(f instanceof class_141){var count=(SetCountLootFunctionAccessor)f;var min=minimum(count.getCountRange());if(min==null||min<(count.getAdd()?0:1))return false;}
  }return true;
 }
 private static List<Double> draws(class_5658 rolls){
  Double min=constant(rolls),max=min;
  if(rolls instanceof class_5662){var u=(UniformLootNumberProviderAccessor)rolls;min=constant(u.getMin());max=constant(u.getMax());}
  if(min==null||max==null||min<0||max<min||max>32||min!=Math.floor(min)||max!=Math.floor(max))return null;
  var dist=new ArrayList<Double>(Collections.nCopies(max.intValue()+1,0.));for(int n=min.intValue();n<=max.intValue();n++)dist.set(n,1/(max-min+1));return dist;
 }
 private static boolean role(Item i){return i.valuable()||(!i.edible()&&!i.id().startsWith("minecraft:")&&RewardRegistry.required(i.id()));}
 public static RewardPlan inspect(class_55 pool,class_5658 bonus){return inspect(pool,bonus,false);}
 public static RewardPlan display(class_55 pool,class_5658 bonus){return inspect(pool,bonus,true);}
 private static RewardPlan inspect(class_55 pool,class_5658 bonus,boolean displayOnly){
  var route=RewardRegistry.route(pool);if(route==null||!Objects.equals(constant(bonus),0.))return null;
  var access=(LootPoolAccessor)pool;if(!functions(access.getFunctions()))return null;
  if(!route.combat()&&Arrays.stream(access.getConditions()).anyMatch(c->!(c instanceof class_219)))return null;
  var items=new ArrayList<Item>();var leaves=new ArrayList<Object>();var weights=new ArrayList<Integer>();var gates=new ArrayList<class_219>();var ids=new HashSet<String>();
  if(!conditions(access.getConditions(),gates))return null;
  for(var raw:access.getEntries()){
   if(!(raw instanceof class_77)&&!(raw instanceof class_73))return null;
   if(!(raw instanceof class_85)||!(raw instanceof LeafEntryAccessor)||!(raw instanceof RewardLeaf))return null;
   var leaf=(LeafEntryAccessor)raw;
   if(((RewardLeaf)raw).prominence$quality()!=0||leaf.getWeight()<=0||!functions(leaf.getFunctions()))return null;
   var conditions=((LootPoolEntryAccessor)raw).getConditions();if(conditions.length>0&&(access.getEntries().length!=1||!conditions(conditions,gates)))return null;
   if(!route.combat()&&Arrays.stream(conditions).anyMatch(c->!(c instanceof class_219)))return null;
   Item item;
   if(raw instanceof class_77){
    var nativeItem=((ItemEntryAccessor)raw).getItem();String id=class_7923.field_41178.method_10221(nativeItem).toString();boolean valuable=nativeItem.method_7841()>0||nativeItem.method_7882()==1;
    if(id.equals("minecraft:book")&&Arrays.stream(leaf.getFunctions()).anyMatch(f->functionId(f).equals("minecraft:enchant_randomly")||functionId(f).equals("minecraft:enchant_with_levels"))){id="minecraft:enchanted_book";valuable=true;}
    if(!ids.add(id))return null;item=new Item(id,leaf.getWeight(),valuable,nativeItem.method_19264()!=null);
   }else item=new Item("minecraft:air",leaf.getWeight(),false,false);
   items.add(item);leaves.add(raw);weights.add(leaf.getWeight());
  }
  long total=weights.stream().mapToLong(Integer::longValue).sum();if(items.isEmpty()||items.size()>128||total>Integer.MAX_VALUE)return null;
  Object gate;String kind;double probability;var rolls=access.getRolls();List<Double> distribution=List.of();var boosted=new ArrayList<Boolean>();
  if(rolls instanceof class_40&&gates.isEmpty()){
   var b=(BinomialLootNumberProviderAccessor)rolls;if(!Objects.equals(constant(b.getN()),1.))return null;
   var p=constant(b.getP());if(p==null)return null;probability=p;gate=rolls;kind="binomial";
  }else if(Objects.equals(constant(rolls),1.)&&gates.size()==1){gate=gates.get(0);probability=((RandomChanceLootConditionAccessor)gate).getChance();kind="chance";}
  else if(gates.isEmpty()){
   if(displayOnly)return null;
   distribution=draws(rolls);if(distribution==null||distribution.size()<2||items.size()<2)return null;
   for(var item:items)boosted.add(!item.id().equals("minecraft:air")&&role(item)&&(double)item.weight()/total<=RewardPolicy.INSTANCE.maximumBase);
   // Scaling every candidate equally cannot improve any probability; leave that pool exact.
   if(boosted.stream().noneMatch(Boolean::booleanValue)||boosted.stream().allMatch(Boolean::booleanValue))return null;
   return new RewardPlan(route,null,"weighted",1,items,leaves,weights,boosted,distribution,true);
  }else return null;
  if(!Double.isFinite(probability)||probability<=0||probability>(displayOnly?1:RewardPolicy.INSTANCE.maximumBase+1e-7))return null;
  if(displayOnly&&items.stream().noneMatch(Item::valuable))return null;
  return new RewardPlan(route,gate,kind,probability,items,leaves,weights,List.of(),distribution,!displayOnly&&items.stream().anyMatch(RewardPlan::role));
 }
 private static boolean conditions(class_5341[] conditions,List<class_219> gates){
  for(var c:conditions){if(c instanceof class_219){gates.add((class_219)c);continue;}
   var id=class_7923.field_41135.method_10221(c.method_29325());if(id==null||!id.method_12836().equals("minecraft")||!Set.of("killed_by_player","entity_properties","damage_source_properties","location_check","match_tool").contains(id.method_12832()))return false;
  }return true;
 }
}
