from pathlib import Path
import argparse,json,os,subprocess
from build import safe_arg
p=argparse.ArgumentParser();p.add_argument('--output',type=Path,required=True);p.add_argument('--classpath',type=Path,required=True);p.add_argument('--java-home',type=Path,required=True);a=p.parse_args();r=Path(__file__).resolve().parents[1];w=a.output.resolve();src=w/'stubs';c=w/'classes';c.mkdir(parents=True,exist_ok=True)
stubs={
'net/minecraft/class_5658':'public interface class_5658 {}',
'net/minecraft/class_44':'public class class_44 implements class_5658,fzzyhmstrs.emi_loot.mixins.ConstantLootNumberProviderAccessor {float v;public class_44(float v){this.v=v;}public float getValue(){return v;}}',
'net/minecraft/class_40':'public class class_40 implements class_5658,fzzyhmstrs.emi_loot.mixins.BinomialLootNumberProviderAccessor {class_5658 n,p;public class_40(class_5658 n,class_5658 p){this.n=n;this.p=p;}public class_5658 getN(){return n;}public class_5658 getP(){return p;}}',
'net/minecraft/class_5662':'public class class_5662 implements class_5658,fzzyhmstrs.emi_loot.mixins.UniformLootNumberProviderAccessor {class_5658 min,max;public class_5662(class_5658 a,class_5658 b){min=a;max=b;}public class_5658 getMin(){return min;}public class_5658 getMax(){return max;}}',
'net/minecraft/class_5341':'public class class_5341 {String id;public class_5341(String id){this.id=id;}public Object method_29325(){return id;}}',
'net/minecraft/class_219':'public class class_219 extends class_5341 implements fzzyhmstrs.emi_loot.mixins.RandomChanceLootConditionAccessor {float p;public class_219(float p){super("minecraft:random_chance");this.p=p;}public float getChance(){return p;}}',
'net/minecraft/class_117':'public class class_117 {String id;public class_117(String id){this.id=id;}public Object method_29321(){return id;}}',
'net/minecraft/class_141':'public class class_141 extends class_117 implements fzzyhmstrs.emi_loot.mixins.SetCountLootFunctionAccessor {class_5658 count;boolean add;public class_141(class_5658 c,boolean a){super("minecraft:set_count");count=c;add=a;}public class_5658 getCountRange(){return count;}public boolean getAdd(){return add;}}',
'net/minecraft/class_85':'public class class_85 extends class_79 implements fzzyhmstrs.emi_loot.mixins.LeafEntryAccessor,fzzyhmstrs.emi_loot.mixins.LootPoolEntryAccessor,local.prominence.rewards.RewardLeaf {public int weight,quality;public class_117[] functions=new class_117[0];public class_5341[] conditions=new class_5341[0];public class_85(int w,int q){weight=w;quality=q;}public int getWeight(){return weight;}public int prominence$quality(){return quality;}public class_117[] getFunctions(){return functions;}public class_5341[] getConditions(){return conditions;}}',
'net/minecraft/class_77':'public class class_77 extends class_85 implements fzzyhmstrs.emi_loot.mixins.ItemEntryAccessor {public class_1792 item;public class_77(class_1792 i,int w,int q){super(w,q);item=i;}public class_1792 getItem(){return item;}}',
'net/minecraft/class_79':'public class class_79 {}',
'net/minecraft/class_65':'public class class_65 extends class_79 {}',
'net/minecraft/class_73':'public class class_73 extends class_85 {public class_73(int w){super(w,0);}}',
'net/minecraft/class_55':'public class class_55 implements fzzyhmstrs.emi_loot.mixins.LootPoolAccessor {public class_79[] entries=new class_79[0];public class_117[] functions=new class_117[0];public class_5341[] conditions=new class_5341[0];public class_5658 rolls=new class_44(1);public class_79[] getEntries(){return entries;}public class_117[] getFunctions(){return functions;}public class_5341[] getConditions(){return conditions;}public class_5658 getRolls(){return rolls;}}',
'net/minecraft/class_1792':'public class class_1792 {public String id;public int damage,count;public boolean edible;public class_1792(String s,int d,int c,boolean e){id=s;damage=d;count=c;edible=e;}public int method_7841(){return damage;}public int method_7882(){return count;}public Object method_19264(){return edible?this:null;}}',
'net/minecraft/class_2960':'public class class_2960 {String id;public class_2960(String id){this.id=id;}public String toString(){return id;}public String method_12836(){return id.split(":")[0];}public String method_12832(){return id.split(":")[1];}}',
'net/minecraft/class_7923':'public class class_7923 {public static Registry field_41178=new Registry(),field_41134=new Registry(),field_41135=new Registry();public static class Registry {public class_2960 method_10221(Object o){return new class_2960(o instanceof class_1792 i?i.id:o.toString());}}}',
'net/minecraft/class_1657':'public class class_1657 {public int looting;double luck;public class_1657(int l,double u){looting=l;luck=u;}public float method_7292(){return (float)luck;}}',
'net/minecraft/class_1890':'public class class_1890 {public static int method_8226(class_1657 p){return p.looting;}}',
'net/minecraft/class_169':'public class class_169<T> {}',
'net/minecraft/class_181':'public class class_181 {public static class_169<class_1657> field_1233=new class_169<>();public static class_169<Object> field_1230=new class_169<>();}',
'net/minecraft/class_47':'public class class_47 {public class_1657 actor;public float luck;public <T> T method_296(class_169<T> p){return (T)actor;}public float method_302(){return luck;}}',
'local/prominence/lootcredit/CreditScope':'public class CreditScope {public static boolean anonymous;public static CreditScope current(){return new CreditScope();}public boolean anonymous(){return anonymous;}}',
'local/prominence/rewards/RewardRegistry':'public class RewardRegistry {public record Route(String table,boolean combat,java.util.Set<String> uniqueItems){}public static java.util.Set<String> required=new java.util.HashSet<>();static java.util.Map<net.minecraft.class_55,Route> routes=new java.util.IdentityHashMap<>();public static void put(net.minecraft.class_55 p,boolean combat){routes.put(p,new Route("test:"+(combat?"entities/rare":"chests/rare"),combat,java.util.Set.of()));}public static Route route(net.minecraft.class_55 p){return routes.get(p);}public static boolean required(String s){return required.contains(s);}}'
}
interfaces={'ConstantLootNumberProviderAccessor':'float getValue();','BinomialLootNumberProviderAccessor':'net.minecraft.class_5658 getN();net.minecraft.class_5658 getP();','UniformLootNumberProviderAccessor':'net.minecraft.class_5658 getMin();net.minecraft.class_5658 getMax();','RandomChanceLootConditionAccessor':'float getChance();','SetCountLootFunctionAccessor':'net.minecraft.class_5658 getCountRange();boolean getAdd();','LeafEntryAccessor':'int getWeight();net.minecraft.class_117[] getFunctions();','LootPoolEntryAccessor':'net.minecraft.class_5341[] getConditions();','ItemEntryAccessor':'net.minecraft.class_1792 getItem();','LootPoolAccessor':'net.minecraft.class_79[] getEntries();net.minecraft.class_117[] getFunctions();net.minecraft.class_5341[] getConditions();net.minecraft.class_5658 getRolls();'}
for n,b in interfaces.items():stubs['fzzyhmstrs/emi_loot/mixins/'+n]='public interface '+n+' {'+b+'}'
for n,b in stubs.items():
 f=src/(n+'.java');f.parent.mkdir(parents=True,exist_ok=True);f.write_text('package '+n.rsplit('/',1)[0].replace('/','.')+';'+b,'utf8')
sources=list(src.rglob('*.java'))+[r/'src/main/java/local/prominence/rewards'/n for n in ['RewardMath.java','RewardDefinition.java','RewardPlan.java','RewardPolicy.java','RewardScope.java','RewardLeaf.java']]+[r/'tests/native/rewardverification/RewardBehavior.java']
cp=os.pathsep.join(json.loads(a.classpath.read_text('utf8')))
opts=['--release','17','-encoding','UTF-8','-proc:none','-cp',cp,'-d',safe_arg(c),*[safe_arg(x) for x in sources]];af=w/'compile.args';af.write_text('\n'.join('"'+x.replace('\\','/')+'"' for x in opts),'utf8')
def run(cmd,name):
 cmd=list(map(str,cmd))
 if cmd[0].endswith('java.exe'):
  arg=w/(name+'.args');arg.write_text('\n'.join('"'+v.replace('\\','/')+'"' for v in cmd[1:]),'utf8');cmd=[cmd[0],'@'+safe_arg(arg)]
 q=subprocess.run(cmd,cwd=w,capture_output=True,text=True,encoding='utf8',errors='replace');(w/name).write_text(q.stdout+q.stderr,'utf8');assert q.returncode==0,q.stdout+q.stderr;print(q.stdout[-2000:])
run([a.java_home/'bin/javac.exe','@'+safe_arg(af)],'compile.log')
run([a.java_home/'bin/java.exe','-cp',os.pathsep.join([safe_arg(c),safe_arg(r/'src/main/resources'),cp]),'rewardverification.RewardBehavior'],'behavior.log')
(w/'report.json').write_text(json.dumps({'status':'passed','productionPolicyPlanScopeExecuted':True,'minecraftApiDoubles':True,'gameStarted':False},indent=2)+'\n','utf8')
