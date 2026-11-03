"""Exercise production replacement source with real native NBT and isolated item/registry stand-ins."""
from pathlib import Path
import argparse,json,os,subprocess,ctypes
p=argparse.ArgumentParser()
for n in ['build-dir','classpath','java-home']:p.add_argument('--'+n,type=Path,required=True)
a=p.parse_args();r=Path(__file__).resolve().parents[1];out=a.build_dir/'infusion-fixture';src=out/'src';classes=out/'classes';classes.mkdir(parents=True,exist_ok=True)
def put(n,s):
 f=src/n;f.parent.mkdir(parents=True,exist_ok=True);f.write_text(s,'utf8')
stubs={
'class_1792':'public class class_1792 {}',
'class_1304':'public enum class_1304 { CHEST; public String method_5923(){return "chest";} }',
'class_1738':'public class class_1738 extends class_1792 {public class_1304 method_7685(){return class_1304.CHEST;}}',
'class_2960':'public record class_2960(String id) {public String toString(){return id;} public String method_12832(){return id.substring(id.indexOf(58)+1);}}',
'class_1320':'public class class_1320 {}',
'class_1322':'public record class_1322(java.util.UUID uuid,String name,double amount,class_1323 operation) { public enum class_1323 { ADDITION } }',
'class_2378':'public class class_2378<T> {public java.util.Map<class_2960,T> values=new java.util.HashMap<>();public T method_10223(class_2960 id){return values.get(id);}public class_2960 method_10221(T item){return values.entrySet().stream().filter(e->e.getValue()==item).findFirst().orElseThrow().getKey();}}',
'class_7923':'public class class_7923 {public static class_2378<class_1792> field_41178=new class_2378<>(); public static class_2378<class_1320> field_41190=new class_2378<>();}',
'class_1799':'''public class class_1799 {
 public class_1792 item;public class_2487 nbt;public class_1799(class_1792 i){item=i;}
 public boolean method_7960(){return item==null;}public class_1792 method_7909(){return item;}
 public class_2487 method_7969(){return nbt;}public class_2487 method_7911(String k){if(nbt==null)nbt=new class_2487();if(!nbt.method_10573(k,10))nbt.method_10566(k,new class_2487());return nbt.method_10562(k);}
 public class_1799 method_7972(){var c=new class_1799(item);c.nbt=nbt==null?null:nbt.method_10553();return c;}
 public void method_7916(class_1320 a,class_1322 m,class_1304 slot){var e=new class_2487();e.method_10582("AttributeName",class_7923.field_41190.method_10221(a).toString());e.method_10582("Name",m.name());e.method_10582("Slot",slot.method_5923());e.method_10549("Amount",m.amount());var l=nbt.method_10554("AttributeModifiers",10);l.add(e);nbt.method_10566("AttributeModifiers",l);}
 }'''
}
for n,s in stubs.items():put('net/minecraft/'+n+'.java','package net.minecraft; '+s)
put('elocindev/eldritch_end/api/infusion/InfusionAttributeHolder.java','''package elocindev.eldritch_end.api.infusion;import net.minecraft.*;public class InfusionAttributeHolder {public class_1320 attribute;public double amount;public class_1322.class_1323 operation=class_1322.class_1323.ADDITION;public InfusionAttributeHolder(class_1320 a,double v){attribute=a;amount=v;}}''')
put('elocindev/eldritch_end/api/infusion/InfusableItemMaterial.java','''package elocindev.eldritch_end.api.infusion;import net.minecraft.*;public interface InfusableItemMaterial {java.util.List<InfusionAttributeHolder> getInfusionAttributes();class_1792 getInfusionTemplate();boolean applyToArmor();boolean isInfusable();default java.util.UUID getInfusionUUID(class_1799 s,class_1304 slot){return java.util.UUID.nameUUIDFromBytes(slot.name().getBytes());}}''')
put('elocindev/eldritch_end/api/infusion/InfusionTemplate.java','package elocindev.eldritch_end.api.infusion;public interface InfusionTemplate {boolean isEquipmentAllowed(net.minecraft.class_1799 stack);}')
put('infusionfixture/Probe.java',(r/'tests/infusion/Probe.java').read_text('utf8'))
cp=json.loads(a.classpath.read_text('utf8'));options=['--release','17','-encoding','UTF-8','-proc:none','-cp',os.pathsep.join(cp),'-d',str(classes),*map(str,src.rglob('*.java')),str(r/'src/main/java/local/prominence/infusion/InfusionReplacement.java')]
def run(options,name,exe):
 f=out/(name+'.args');f.write_text('\n'.join('"'+str(s).replace('\\','/')+'"' for s in options),'utf8');x=subprocess.run([str(a.java_home/('bin/'+exe+'.exe')),'@'+str(f)],capture_output=True,text=True,encoding='utf8',errors='replace');(out/(name+'.log')).write_text(x.stdout+x.stderr,'utf8');assert x.returncode==0,x.stdout+x.stderr;print(x.stdout[-1800:])
run(options,'compile','javac')
def short(s):
 b=ctypes.create_unicode_buffer(32768);s=str(Path(s).resolve());return b.value if ctypes.windll.kernel32.GetShortPathNameW(s,b,len(b)) else s
run(['-Xmx256m','-ea','-cp',os.pathsep.join(short(x) for x in [classes,*cp]),'infusionfixture.Probe'],'fixture','java')
