package local.prominence.rewards;
import net.minecraft.*;
public final class RewardScope implements AutoCloseable {
 private static final ThreadLocal<RewardScope> CURRENT=new ThreadLocal<>();
 private final RewardScope previous;final RewardPlan plan;final class_47 context;final int looting;final double luck;
 private final java.util.IdentityHashMap<Object,Integer> weights=new java.util.IdentityHashMap<>();
 private RewardScope(RewardPlan plan,class_47 context){previous=CURRENT.get();this.plan=plan;this.context=context;
  var actor=context.method_296(class_181.field_1233);var killer=context.method_296(class_181.field_1230);
  if(killer instanceof class_1657 p)actor=p;
  var credit=local.prominence.lootcredit.CreditScope.current();if(credit!=null&&credit.anonymous())actor=null;
  // Native helper includes the existing originating-weapon attribution fix.
  looting=plan.combat()&&actor!=null?Math.max(0,class_1890.method_8226(actor)):0;
  luck=plan.combat()?(actor==null?0:actor.method_7292()):context.method_302();
  if(plan.weighted()){
   int[] raw=plan.weights().stream().mapToInt(Integer::intValue).toArray();boolean[] boost=new boolean[raw.length];for(int i=0;i<raw.length;i++)boost[i]=plan.boosts().get(i);
   var p=RewardPolicy.INSTANCE;var result=RewardMath.weights(raw,boost,looting,luck,plan.combat()?p.lootingRate:0,p.luckRate);for(int i=0;i<result.length;i++)weights.put(plan.leaves().get(i),result[i]);
  }CURRENT.set(this);
 }
 public static RewardScope enter(RewardPlan p,class_47 c){return new RewardScope(p,c);}
 public static float probability(Object gate,class_47 context,float original){var s=CURRENT.get();if(s==null||s.context!=context||s.plan.gate()!=gate)return original;return (float)RewardMath.chance(original,s.looting,s.luck,s.plan.combat()?RewardPolicy.INSTANCE.lootingRate:0,RewardPolicy.INSTANCE.luckRate);}
 public static int weight(Object leaf,int original){var s=CURRENT.get();return s==null||!s.plan.weighted()?original:s.weights.getOrDefault(leaf,original);}
 public void close(){if(previous==null)CURRENT.remove();else CURRENT.set(previous);}
}
