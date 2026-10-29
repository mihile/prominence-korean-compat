package lootfixture;
import local.prominence.lootcredit.*;import net.minecraft.*;import java.util.UUID;
public class LootBehavior {
 static void check(boolean x,String m){if(!x)throw new AssertionError(m);}
 public static void main(String[] args){var w=new class_1937();var a=new class_1657(w);var b=new class_1657(w);var victim=new class_1309(w);w.players.put(a.id,a);w.players.put(b.id,b);a.main=new class_1799(3,2);b.main=new class_1799(1,0);
 var launched=Credit.capture(a,a.main);var projectile=new Projectile(w,launched);a.main=new class_1799(0,0);var direct=new class_1282(new class_6880(),projectile,a);
 check(LootCredit.damage(victim,direct).equals(launched),"thrown weapon snapshot lost");
 a.main=new class_1799(5,3);check(LootCredit.damage(victim,direct).equals(launched),"later stronger weapon changed projectile bonus");
 var none=new class_1282(new class_6880(),null,null);check(LootCredit.damage(victim,none)==null,"environment credited");
 try(var s=CreditScope.enter(launched,true)){
  var c=LootCredit.damage(victim,none);check(c.equals(launched),"scoped extra hit lost owner");var loot=LootCredit.attributed(victim,none,c);check(loot.method_5529()==a&&loot.method_48793()==none.method_48793(),"loot source/type");check(none.method_5529()==null,"original combat source mutated");
  check(LootCredit.damage(a,none)==null,"self-damage credited");
  var counter=new class_1282(new class_6880(),b,b);check(LootCredit.damage(a,counter).owner().equals(b.id),"reaction credited to outer attacker");
  check(LootCredit.dropCredit(b)==null,"unrelated player got bonuses");check(LootCredit.dropCredit(a).equals(launched),"loot event lost snapshot");
 }
 try(var s=CreditScope.enter(launched,false)){check(LootCredit.damage(victim,none)==null,"unscoped anonymous damage inherited");}
 w.players.remove(a.id);try(var s=CreditScope.enter(launched,true)){check(LootCredit.damage(victim,none)==null,"offline owner synthesized");}
 check(CreditScope.current()==null,"thread scope leak");System.out.println("PASS: actual LootCredit logic: thrown/weapon-switch snapshots, scoped extra hit, original damage/type preservation, environment/self/reactive/offline isolation and scavenger player matching");
 }
 static class Projectile extends class_1297 implements CreditHolder {Credit c;Projectile(class_1937 w,Credit c){super(w);this.c=c;}public Credit prominence$credit(){return c;}public void prominence$credit(Credit v){c=v;}}
}
