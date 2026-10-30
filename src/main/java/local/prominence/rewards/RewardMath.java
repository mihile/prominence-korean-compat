package local.prominence.rewards;

/** One probability gate, no extra roll or drop duplication. */
public final class RewardMath {
 private RewardMath(){}
 public static double chance(double base,int looting,double luck,double lootRate,double luckRate){
  if(!Double.isFinite(base)||base<0||base>1)throw new IllegalArgumentException("base chance");
  if(!Double.isFinite(luck))luck=0;
  double factor=1+Math.max(0,looting)*lootRate+Math.max(0,luck)*luckRate;
  return Math.min(1,base*factor);
 }
 public static int[] weights(int[] raw,boolean[] boost,int looting,double luck,double lootRate,double luckRate){
  if(raw.length!=boost.length||raw.length==0||raw.length>128)throw new IllegalArgumentException("weights");
  if(!Double.isFinite(luck))luck=0;double factor=1+Math.max(0,looting)*lootRate+Math.max(0,luck)*luckRate;
  var result=raw.clone();if(factor==1)return result;
  double sum=0;for(int i=0;i<raw.length;i++){if(raw[i]<0)throw new IllegalArgumentException("negative weight");sum+=raw[i]*(boost[i]?factor:1);}
  int scale=(int)Math.min(1000,Math.floor((Integer.MAX_VALUE-raw.length-1)/Math.max(1,sum)));if(scale<1)return result;
  for(int i=0;i<raw.length;i++)result[i]=(int)Math.round(raw[i]*(boost[i]?factor:1)*scale);
  return result;
 }
 public static double atLeastOne(double share,java.util.List<Double> draws){
  return atLeastOne(share,draws,0,0);
 }
 /** Native LootPool adds floor(bonusRolls * context luck), clamping total draws at zero. */
 public static double atLeastOne(double share,java.util.List<Double> draws,double bonusRolls,double luck){
  if(share<0||share>1)throw new IllegalArgumentException("share");double chance=0;
  double extra=Math.floor((double)(float)((float)bonusRolls*(float)luck));
  for(int n=0;n<draws.size();n++){double count=Math.max(0,n+extra);if(count>0)chance+=draws.get(n)*(share==1?1:-Math.expm1(count*Math.log1p(-share)));}return Math.min(1,chance);
 }
}
