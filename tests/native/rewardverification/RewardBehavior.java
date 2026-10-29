package rewardverification;
import local.prominence.rewards.*;import net.minecraft.*;import java.util.*;
public final class RewardBehavior {
 static int checks;static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}static void near(double a,double b,String why){check(Math.abs(a-b)<1e-6,why+" "+a+" != "+b);}
 static class_55 gate(String id,float chance,boolean combat){var p=new class_55();p.entries=new class_85[]{new class_77(new class_1792(id,0,64,false),1,0)};p.rolls=new class_40(new class_44(1),new class_44(chance));RewardRegistry.put(p,combat);return p;}
 static RewardPlan plan(class_55 p){return RewardPlan.inspect(p,new class_44(0));}
 public static void main(String[] args){
  RewardRegistry.required.add("test:rare");var p=gate("test:rare",.1f,true);var plan=plan(p);check(plan!=null&&plan.eligible(),"referenced material");
  var c=new class_47();c.actor=new class_1657(3,10);try(var ignored=RewardScope.enter(plan,c)){near(RewardScope.probability(p.rolls,c,.1f),.27,"actual production scope loot+luck");near(RewardScope.probability(new Object(),c,.1f),.1,"other gate not modified");near(RewardScope.probability(p.rolls,new class_47(),.1f),.1,"other context not modified");}
  near(RewardScope.probability(p.rolls,c,.1f),.1,"scope cleaned");
  var q=gate("test:rare",.01f,true);var other=plan(q);try(var ignored=RewardScope.enter(plan,c)){try(var nested=RewardScope.enter(other,c)){near(RewardScope.probability(q.rolls,c,.01f),.027,"nested table");near(RewardScope.probability(p.rolls,c,.1f),.1,"parent gate not leaked");}near(RewardScope.probability(p.rolls,c,.1f),.27,"parent restored");}
  try{try(var ignored=RewardScope.enter(plan,c)){throw new RuntimeException();}}catch(RuntimeException expected){}near(RewardScope.probability(p.rolls,c,.1f),.1,"exception cleanup");
  local.prominence.lootcredit.CreditScope.anonymous=true;try(var ignored=RewardScope.enter(plan,c)){near(RewardScope.probability(p.rolls,c,.1f),.1,"no unrelated-player bonus");}local.prominence.lootcredit.CreditScope.anonymous=false;
  c.actor=null;try(var ignored=RewardScope.enter(plan,c)){near(RewardScope.probability(p.rolls,c,.1f),.1,"environment kill stays original");}
  var chest=gate("test:rare",.1f,false);c.actor=new class_1657(10,100);c.luck=10;try(var ignored=RewardScope.enter(plan(chest),c)){near(RewardScope.probability(chest.rolls,c,.1f),.15,"chest uses context luck, never Looting");}
  check(!plan(gate("test:food",.1f,true)).eligible(),"unreferenced common item untouched");
  var food=gate("test:rare",.1f,true);((class_77)food.entries[0]).item.edible=true;check(!plan(food).eligible(),"ordinary food untouched");
  check(plan(gate("test:rare",.5f,true))==null,"common chance excluded");
  var gear=gate("test:sword",.01f,true);((class_77)gear.entries[0]).item.damage=100;check(plan(gear).eligible(),"equipment discovered without item allowlist");
  check(RewardPlan.inspect(p,new class_44(1))==null,"native bonus rolls protected");
  ((class_85)p.entries[0]).quality=1;check(plan(p)==null,"native luck quality protected");((class_85)p.entries[0]).quality=0;
  ((class_85)p.entries[0]).functions=new class_117[]{new class_117("minecraft:looting_enchant")};check(plan(p)==null,"native Looting function protected");((class_85)p.entries[0]).functions=new class_117[0];
  p.conditions=new class_5341[]{new class_5341("minecraft:random_chance_with_looting")};check(plan(p)==null,"native Looting condition protected");
  p.conditions=new class_5341[]{new class_5341("foreign:condition")};check(plan(p)==null,"unknown predicate stays native");p.conditions=new class_5341[0];
  p.functions=new class_117[]{new class_141(new class_5662(new class_44(0),new class_44(2)),false)};check(plan(p)==null,"zero-output set_count cannot claim true drop chance");p.functions=new class_117[0];
  var chance=new class_219(.1f);p.rolls=new class_44(1);p.conditions=new class_5341[]{chance};check(plan(p).gate()==chance,"chance condition identity");
  var weighted=new class_55();weighted.entries=new class_85[]{new class_77(new class_1792("test:common",0,64,false),9,0),new class_77(new class_1792("test:sword",100,1,false),1,0)};weighted.rolls=new class_5662(new class_44(1),new class_44(2));RewardRegistry.put(weighted,true);
  var wp=plan(weighted);check(wp!=null&&wp.weighted()&&wp.boosts().equals(List.of(false,true)),"weighted rare equipment classification");c.actor=new class_1657(3,0);try(var ignored=RewardScope.enter(wp,c)){int a=RewardScope.weight(weighted.entries[0],9),b=RewardScope.weight(weighted.entries[1],1);near((double)b/(a+b),2.2/11.2,"weighted normalization");check(b==RewardScope.weight(weighted.entries[1],1),"two native weight passes same result");check(RewardScope.weight(new Object(),17)==17,"unrelated selection untouched");}
  check(RewardScope.weight(weighted.entries[1],1)==1,"weight scope cleared");
  check(wp.definition().valid(),"server definition reflects production plan");
  var all=new class_55();all.entries=new class_85[10];for(int i=0;i<10;i++)all.entries[i]=new class_77(new class_1792("test:gear"+i,100,1,false),1,0);RewardRegistry.put(all,true);check(plan(all)==null,"equally boosted all-rare pool stays native, no useless RNG domain change");
  var dup=new class_55();dup.entries=new class_79[]{weighted.entries[1],weighted.entries[1]};RewardRegistry.put(dup,true);check(plan(dup)==null,"duplicate item/NBT ambiguity stays native");
  var alternative=new class_55();alternative.entries=new class_79[]{new class_65()};RewardRegistry.put(alternative,true);check(plan(alternative)==null,"actual AlternativeEntry cannot be cast to leaf");
  var empty=new class_55();empty.entries=new class_79[]{new class_73(9),new class_77(new class_1792("test:rare",0,64,false),1,0)};RewardRegistry.put(empty,true);check(plan(empty)!=null,"actual EmptyEntry remains a weighted leaf");
  var nativeGear=gate("test:sword",.3f,true);((class_77)nativeGear.entries[0]).item.damage=100;nativeGear.rolls=new class_44(1);nativeGear.conditions=new class_5341[]{new class_219(.3f)};
  check(plan(nativeGear)==null,"native 30% pool not silently buffed");var display=RewardPlan.display(nativeGear,new class_44(0));check(display!=null&&!display.eligible()&&display.definition().valid()&&display.definition().lootingRate()==0,"native gear probability independently displayable");
  var common=gate("minecraft:leather",.5f,true);var commonDisplay=RewardPlan.display(common,new class_44(0));check(commonDisplay!=null&&!commonDisplay.eligible()&&commonDisplay.definition().lootingRate()==0,"common materials included for display without bonus policy change");
  var guaranteed=new class_55();guaranteed.entries=new class_79[]{new class_77(new class_1792("minecraft:bone",0,64,false),1,0)};RewardRegistry.put(guaranteed,true);var certain=RewardPlan.display(guaranteed,new class_44(0));check(certain!=null&&certain.definition().valid()&&!certain.eligible(),"guaranteed common drops have native display");near(RewardMath.atLeastOne(1,certain.draws()),1,"guaranteed percentage");
  var unscaled=RewardPlan.display(weighted,new class_44(0));check(unscaled!=null&&!unscaled.eligible()&&unscaled.boosts().stream().noneMatch(Boolean::booleanValue),"weighted native display never boosts weights");near(RewardMath.atLeastOne(.1,unscaled.draws()),.145,"native one-or-two draws actual probability");
  check(RewardPlan.display(alternative,new class_44(0))==null,"unknown composite does not manufacture a probability");check(RewardPlan.display(p,new class_44(1))==null,"unknown native luck terms remain conservative for display");
  System.out.println("PASS "+checks+" production eligibility, exact scope, live gear, anonymous/environment kills, chest context, nested/exception cleanup, native bonus protections, equipment discovery and normalized weights; API doubles only");
 }
}
