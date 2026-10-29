from pathlib import Path
import argparse,subprocess,json
from build import safe_arg
p=argparse.ArgumentParser();p.add_argument('--output',type=Path,required=True);p.add_argument('--java-home',type=Path,required=True);a=p.parse_args();r=Path(__file__).resolve().parents[1];w=a.output;src=w/'stubs/net/minecraft';src.mkdir(parents=True,exist_ok=True);c=w/'classes';c.mkdir(exist_ok=True)
stubs={
'class_1937':'public boolean field_9236;public java.util.Map<java.util.UUID,class_1657> players=new java.util.HashMap<>();public class_1657 method_18470(java.util.UUID id){return players.get(id);}',
'class_1297':'public java.util.UUID id=java.util.UUID.randomUUID();public class_1937 world;public class_1297(class_1937 w){world=w;}public class_1937 method_37908(){return world;}public java.util.UUID method_5667(){return id;}',
'class_1309 extends class_1297':'public class_1799 main=new class_1799(0,0);public class_1309(class_1937 w){super(w);}public class_1799 method_6047(){return main;}public class_1799 method_6079(){return main;}',
'class_1657 extends class_1309':'public class_1657(class_1937 w){super(w);}',
'class_1799':'public int loot,scav;public class_1799(int l,int s){loot=l;scav=s;}',
'class_1887':'public boolean loot;public class_1887(boolean l){loot=l;}',
'class_1893':'public static class_1887 field_9110=new class_1887(true);',
'class_1890':'public static int method_8225(class_1887 e,class_1799 s){return e.loot?s.loot:s.scav;}',
'class_2960':'String id;public class_2960(String a,String b){id=a+":"+b;}public String toString(){return id;}',
'class_7923':'public static Registry field_41176=new Registry(),field_41174=new Registry();public static class Registry {public class_1887 method_10223(class_2960 id){return new class_1887(false);}public class_2960 method_10221(Object o){return new class_2960("fake","effect");}}',
'class_1293':'public class_1291 method_5579(){return new class_1291();}',
'class_1291':'',
'class_6880':'',
'class_1282':'class_6880 type;class_1297 direct,actor;public class_1282(class_6880 t,class_1297 d,class_1297 a){type=t;direct=d;actor=a;}public class_1297 method_5529(){return actor;}public class_1297 method_5526(){return direct;}public class_6880 method_48793(){return type;}',
'class_2487':'public void method_25927(String k,java.util.UUID id){}public void method_10569(String k,int i){}public void method_10566(String k,class_2487 n){}public boolean method_10545(String k){return false;}public class_2487 method_10562(String k){return this;}public boolean method_25928(String k){return false;}public java.util.UUID method_25926(String k){return null;}public int method_10550(String k){return 0;}'
}
for name,body in stubs.items():(src/(name.split()[0]+'.java')).write_text('package net.minecraft;public class '+name+'{'+body+'}')
sources=list(src.glob('*.java'))+[r/'src/main/java/local/prominence/lootcredit'/n for n in ['Credit.java','CreditHolder.java','CreditScope.java','LootCredit.java']]+[r/'tests/native/lootfixture/LootBehavior.java']
opts=['--release','17','-encoding','UTF-8','-d',safe_arg(c),*[safe_arg(s) for s in sources]];af=w/'compile.args';af.write_text('\n'.join('"'+x+'"' for x in opts))
q=subprocess.run([str(a.java_home/'bin/javac.exe'),'@'+safe_arg(af)],capture_output=True,text=True);(w/'compile.log').write_text(q.stdout+q.stderr);assert q.returncode==0,q.stderr
q=subprocess.run([str(a.java_home/'bin/java.exe'),'-cp',safe_arg(c),'lootfixture.LootBehavior'],capture_output=True,text=True);(w/'behavior.log').write_text(q.stdout+q.stderr);assert q.returncode==0,q.stdout+q.stderr;print(q.stdout)
(w/'report.json').write_text(json.dumps({'status':'passed','productionLootLogicExecuted':True,'minecraftApiDoubles':True,'gameStarted':False},indent=2)+'\n')
