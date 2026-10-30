package local.prominence.rewards;
import java.util.*;import java.nio.charset.StandardCharsets;import local.prominence.rewards.client.*;
public final class RewardScalingTests {
 static int checks;static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}static void near(double a,double b,String why){check(Math.abs(a-b)<1e-10,why+" "+a+" != "+b);}
 static RewardDefinition gate(boolean mob){return new RewardDefinition("test:entities/rare","binomial",.1,mob?.4:0,.05,mob,List.of(new RewardDefinition.Output("test:material",1,10,0,true)),List.of(),List.of(),List.of());}
 public static void main(String[] args){
  near(RewardMath.chance(.1,3,0,.4,.05),.22,"calibration");near(RewardMath.chance(.1,3,10,.4,.05),.27,"bonuses combine once");near(RewardMath.chance(.01,3,10,.4,.05),.027,"relative rare chance");near(RewardMath.chance(.1,0,-30,.4,.05),.1,"no new negative-luck penalty");near(RewardMath.chance(.2,200,100,.4,.05),1,"cap");
  for(int l=0;l<16;l++)for(int u=0;u<64;u++)for(double p:new double[]{.000001,.001,.01,.1,.2}){double c=RewardMath.chance(p,l,u,.4,.05);check(c>=p&&c<=1,"bounded bonus");check(c>=RewardMath.chance(p,Math.max(0,l-1),u,.4,.05),"Looting monotonic");check(c>=RewardMath.chance(p,l,Math.max(0,u-1),.4,.05),"Luck monotonic");}
  for(double p:new double[]{(double).1f,(double).01f,(double).15f})check(Double.doubleToLongBits(RewardMath.chance(p,0,0,.4,.05))==Double.doubleToLongBits(p),"zero-bonus exact threshold");
  int[] raw={9,1};boolean[] boost={false,true};check(Arrays.equals(raw,RewardMath.weights(raw,boost,0,0,.4,.05)),"zero-bonus exact RNG weight domain");
  int[] w=RewardMath.weights(raw,boost,3,0,.4,.05);near((double)w[1]/(w[0]+w[1]),2.2/11.2,"weighted denominator");check(Arrays.equals(w,RewardMath.weights(raw,boost,3,0,.4,.05)),"two native weight passes deterministic");
  near(RewardMath.atLeastOne(.1,List.of(0.,0.,1.)),.19,"two draws are not a mean");near(RewardMath.atLeastOne(1,List.of(.5,.5)),.5,"zero draw mass");near(RewardMath.atLeastOne(0,List.of(0.,0.,1.)),0,"no chance");
  check(Arrays.equals(new int[]{Integer.MAX_VALUE-20,1},RewardMath.weights(new int[]{Integer.MAX_VALUE-20,1},new boolean[]{false,true},255,1000,.4,.05)),"overflow preserves original");
  var d=gate(true);check(d.valid(),"definition");check(RewardWire.decode(RewardWire.encode(List.of(d))).equals(List.of(d)),"server-data roundtrip");
  var nativeBonus=new RewardDefinition("test:entities/boss","weighted",1,0,0,true,List.of(new RewardDefinition.Output("test:ring",1./90,200./90,0,true)),Collections.nCopies(90,1),Collections.nCopies(90,false),List.of(0.,0.,1.),(double).2f);
  check(nativeBonus.valid(),"native bonus metadata");check(RewardWire.decode(RewardWire.encode(List.of(d,nativeBonus))).equals(List.of(d,nativeBonus)),"v3 bonus roundtrip");
  check(RewardWire.decode(RewardWire.legacyPayload(List.of(d,nativeBonus))).equals(List.of(d)),"v2 never receives a coefficient it would ignore");
  near(RewardMath.atLeastOne(.1,List.of(.5,.5),(double).2f,5),.145,"native float floor plus zero-draw mass");near(RewardMath.atLeastOne(.1,List.of(0.,0.,1.),(double).2f,-5),.1,"negative native Luck removes draws");
  var nativeLines=RewardClientText.lines(new RewardClientText.Attachment(nativeBonus,nativeBonus.outputs().get(0)),3,5,true,true);
  check(nativeLines.get(0).startsWith("드롭 확률: 3.29%"),"actual ring at-least-one probability "+nativeLines);check(nativeLines.stream().anyMatch(s->s.startsWith("추가 횟수: ")&&s.endsWith("= 1")),"native Luck formula");
  check(nativeLines.stream().noneMatch(s->s.contains("약탈")),"no invented Looting for native bonus pool");
  var second=new RewardDefinition(d.table(),"binomial",.1,.4,.05,true,List.of(new RewardDefinition.Output("test:second",1,10,0,true)),List.of(),List.of(),List.of());
  RewardClientState.install(List.of(d,second));var grouped=new RewardClientText.TooltipSource(d.table(),List.of("test:material","test:second"),10,List.of(),List.of(),List.of());
  var groupedLines=RewardClientText.resolved(grouped,3,10,true,true,id->id);check(groupedLines.get(0).getString().equals("드롭 확률: 27% (기본 10%)"),"coalesced separate pools share a verified tooltip");
  var twoDraw=new RewardDefinition("test:entities/grouped","weighted",1,0,0,true,List.of(new RewardDefinition.Output("test:two_draws",.1,20,1,true)),List.of(9,1),List.of(false,false),List.of(0.,0.,1.));
  var oneDraw=new RewardDefinition(twoDraw.table(),"binomial",.2,0,0,true,List.of(new RewardDefinition.Output("test:one_draw",1,20,0,true)),List.of(),List.of(),List.of());
  RewardClientState.install(List.of(twoDraw,oneDraw));var unequal=new RewardClientText.TooltipSource(twoDraw.table(),List.of("test:two_draws","test:one_draw"),20,List.of(),List.of(),List.of());
  var different=RewardClientText.resolved(unequal,0,0,true,false,id->id).stream().map(net.minecraft.class_2561::getString).toList();check(different.equals(List.of("test:two_draws: 19%","test:one_draw: 20%")),"equal expected counts do not hide distinct probabilities "+different);
  for(String json:List.of("{}","{\"schema\":2,\"plans\":[]}","{\"schema\":1,\"plans\":[{}]}","[".repeat(40)+"]".repeat(40))){try{RewardWire.decode(json.getBytes(StandardCharsets.UTF_8));throw new AssertionError("malformed metadata accepted");}catch(RuntimeException good){checks++;}}
  RewardClientState.install(List.of(d));check(RewardClientState.find(d.table(),List.of("test:material"),10,List.of()).equals(d),"authoritative match");check(RewardClientState.find(d.table(),List.of("other:item"),10,List.of())==null,"unrelated item");check(RewardClientState.find(d.table(),List.of("test:material"),30,List.of())==null,"server-value mismatch");RewardClientState.clear();check(RewardClientState.find(d.table(),List.of("test:material"),10,List.of())==null,"server session clear");check(!RewardClientState.active(d),"stale tooltip metadata invalidated");
  var text=RewardClientText.lines(new RewardClientText.Attachment(d,d.outputs().get(0)),3,10,true,true);check(text.get(0).equals("드롭 확률: 27% (기본 10%)"),"actual live display "+text);check(text.size()==4&&text.get(2).contains("0.4")&&text.get(2).contains("0.05"),"formula from server coefficients");
  var chest=gate(false);check(RewardClientText.lines(new RewardClientText.Attachment(chest,chest.outputs().get(0)),10,10,true,false).get(0).equals("드롭 확률: 15% (기본 10%)"),"chest excludes Looting");
  var weighted=new RewardDefinition("test:chests/gear","weighted",1,0,.05,false,List.of(new RewardDefinition.Output("test:gear",.1,20,1,true)),List.of(9,1),List.of(false,true),List.of(0.,0.,1.));check(weighted.valid(),"weighted metadata");var lines=RewardClientText.lines(new RewardClientText.Attachment(weighted,weighted.outputs().get(0)),3,10,true,true);check(lines.get(0).startsWith("드롭 확률: 26.53%"),"weighted repeated-draw probability "+lines);
  check(RewardPolicy.INSTANCE.quests.size()>1000&&RewardPolicy.INSTANCE.lootingRate==.4&&RewardPolicy.INSTANCE.luckRate==.05,"data policy and quest input");
  System.out.println("PASS "+checks+" reward calibration, bounds, exact zero-bonus, weight/RNG consistency, probability-vs-mean, packet/session/old-server safety and actual UI formula checks; no game");
 }
}
