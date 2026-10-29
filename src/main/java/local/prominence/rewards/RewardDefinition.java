package local.prominence.rewards;
import java.util.*;
/** Authoritative display data sent by the server, never executable expressions. */
public record RewardDefinition(String table,String kind,double base,double lootingRate,double luckRate,boolean combat,List<Output> outputs,List<Integer> weights,List<Boolean> boosts,List<Double> draws){
 public record Output(String item,double share,double expected,int index,boolean unique){}
 public RewardDefinition {outputs=List.copyOf(outputs);weights=List.copyOf(weights);boosts=List.copyOf(boosts);draws=List.copyOf(draws);}
 public boolean weighted(){return kind.equals("weighted");}
 public boolean valid(){
  if(table==null||!table.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")||table.length()>256||!Set.of("chance","binomial","weighted").contains(kind)||!Double.isFinite(base)||base<=0||base>(weighted()?1:.2000001)||!Double.isFinite(lootingRate)||lootingRate<0||lootingRate>1||!Double.isFinite(luckRate)||luckRate<0||luckRate>1||outputs.isEmpty()||outputs.size()>128)return false;
  if(weighted()&&(weights.isEmpty()||weights.size()>128||weights.size()!=boosts.size()||weights.stream().anyMatch(w->w==null||w<0)||weights.stream().mapToLong(Integer::longValue).sum()>Integer.MAX_VALUE||weights.stream().mapToLong(Integer::longValue).sum()==0||draws.size()<2||draws.size()>33||draws.stream().anyMatch(p->p==null||!Double.isFinite(p)||p<0||p>1)||Math.abs(draws.stream().mapToDouble(Double::doubleValue).sum()-1)>.000001))return false;
  return outputs.stream().allMatch(o->o.item()!=null&&o.item().matches("[a-z0-9_.-]+:[a-z0-9_./-]+")&&o.item().length()<=256&&Double.isFinite(o.share())&&o.share()>0&&o.share()<=1&&Double.isFinite(o.expected())&&o.expected()>0&&o.expected()<=3200&&(!weighted()||o.index()>=0&&o.index()<weights.size()));
 }
}
