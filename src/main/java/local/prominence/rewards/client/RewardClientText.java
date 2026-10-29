package local.prominence.rewards.client;
import dev.emi.emi.api.recipe.EmiRecipe;import dev.emi.emi.api.widget.SlotWidget;import fzzyhmstrs.emi_loot.util.ConditionalStack;import local.prominence.rewards.*;import local.prominence.work.*;import net.minecraft.*;import java.math.BigDecimal;import java.util.*;
public final class RewardClientText {
 private record Gear(int looting,double luck){}
 private static final RewardPollCache<Gear> GEAR=new RewardPollCache<>();
 public static void reset(){GEAR.clear();}
 public record Attachment(RewardDefinition definition,RewardDefinition.Output output){}
 private static List<String> items(SlotWidget slot){return slot.getStack().getEmiStacks().stream().map(s->class_7923.field_41178.method_10221(s.getItemStack().method_7909()).toString()).toList();}
 public record TooltipSource(String table,List<String> items,double expected,List<class_2561> conditions,List<class_2561> nativeLines,List<class_2561> otherConditions){
  public TooltipSource{items=List.copyOf(items);conditions=List.copyOf(conditions);nativeLines=List.copyOf(nativeLines);otherConditions=List.copyOf(otherConditions);}
 }
 public static SlotWidget present(SlotWidget slot,double expected,List<class_2561> conditions,EmiRecipe recipe,List<class_2561> fallback,List<class_2561> other){
  if(!(slot instanceof RewardSlot target)){for(var line:fallback)slot.appendTooltip(line);return slot;}
  var id=recipe.getId();target.prominence$source(new TooltipSource(LootDropTooltip.table(id==null?"":id.toString()),items(slot),expected,conditions,fallback,other));return slot;
 }
 public static List<class_2561> current(TooltipSource source){
  var player=class_310.method_1551().field_1724;var d=RewardClientState.find(source.table(),source.items(),source.expected(),source.conditions());
  if(player==null)return source.nativeLines();
  var gear=GEAR.poll(player,System.nanoTime(),()->new Gear(class_1890.method_8226(player),player.method_7292()));
  if(d==null)return LootDropTooltip.currentNative(source,gear.looting(),gear.luck(),LootUiText.korean(),class_437.method_25442());
  var output=d.outputs().stream().filter(o->o.item().equals(source.items().get(0))).findFirst().orElseThrow();
  var out=new ArrayList<class_2561>();for(var line:lines(new Attachment(d,output),gear.looting(),gear.luck(),LootUiText.korean(),class_437.method_25442()))out.add(class_2561.method_43470(line));out.addAll(source.otherConditions());return out;
 }
 private static String percentage(double chance){double percent=new BigDecimal(Float.toString((float)chance)).movePointRight(2).doubleValue();return LootNumbers.percent(percent)+"%";}
 public static List<String> lines(Attachment a,int looting,double luck,boolean ko,boolean formula){
  if(!Double.isFinite(luck))luck=0;
  var d=a.definition();if(!d.combat())looting=0;double adjusted,base;int[] effective=null;
  if(d.weighted()){
   int[] raw=d.weights().stream().mapToInt(Integer::intValue).toArray();boolean[] boosts=new boolean[raw.length];for(int i=0;i<raw.length;i++)boosts[i]=d.boosts().get(i);
   effective=RewardMath.weights(raw,boosts,looting,luck,d.lootingRate(),d.luckRate());long total=0;for(int w:effective)total+=w;
   adjusted=RewardMath.atLeastOne((double)effective[a.output().index()]/total,d.draws());base=RewardMath.atLeastOne(a.output().share(),d.draws());
  }else{adjusted=(double)(float)RewardMath.chance(d.base(),looting,luck,d.lootingRate(),d.luckRate())*a.output().share();base=d.base()*a.output().share();}
  boolean scaled=d.lootingRate()>0||d.luckRate()>0;
  var out=new ArrayList<String>();out.add((ko?"드롭 확률: ":"Drop chance: ")+percentage(adjusted)+(scaled?(ko?" (기본 ":" (base ")+percentage(base)+")":""));
  if(!scaled){if(formula)out.add(d.weighted()?drawFormula(a.output().share(),d.draws(),ko):(ko?"기본 확률 ":"Pool chance ")+percentage(d.base())+(ko?" × 해당 아이템 ":" × item share ")+percentage(a.output().share()));return out;}

  out.add((ko?"현재 장비: ":"Current gear: ")+(d.combat()?(ko?"약탈 ":"Looting ")+Math.max(0,looting)+" · ":"")+(ko?"운 ":"Luck ")+LootNumbers.number(Math.max(0,luck)));
  if(formula){
   if(d.weighted()){long total=0;for(int w:effective)total+=w;out.add((ko?"선택 비중: ":"Selection weight: ")+effective[a.output().index()]+" / "+total);out.add(drawFormula((double)effective[a.output().index()]/total,d.draws(),ko));}
   else out.add((ko?"확률 계산: ":"Gate: ")+percentage(d.base())+" × (1"+(d.combat()?" + "+LootNumbers.number(d.lootingRate())+" × "+Math.max(0,looting):"")+" + "+LootNumbers.number(d.luckRate())+" × "+LootNumbers.number(Math.max(0,luck))+")"+(a.output().share()<1?" × "+percentage(a.output().share()):""));
   if(d.combat())out.add(ko?"수집가의 추가 추첨은 별도로 적용됩니다.":"Scavenger adds a separate loot roll.");
  }
  return out;
 }
 private static String drawFormula(double share,List<Double> draws,boolean ko){var terms=new ArrayList<String>();for(int n=0;n<draws.size();n++)if(draws.get(n)>0)terms.add(LootNumbers.number(draws.get(n))+" × (1 − "+LootNumbers.number(share)+")^"+n);return (ko?"확률 계산: ":"Calculation: ")+"1 − ["+String.join(" + ",terms)+"]";}
}
