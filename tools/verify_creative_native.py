"""Real Mixin/ASM validation plus native AE2 iteration; small Minecraft/factory stand-ins avoid GL/world startup."""
from pathlib import Path
import argparse,json,subprocess,os
p=argparse.ArgumentParser();p.add_argument('--build-dir',type=Path,required=True);p.add_argument('--classpath',type=Path,required=True);p.add_argument('--java-home',type=Path,required=True);a=p.parse_args()
r=Path(__file__).resolve().parents[1];w=a.build_dir/'native-creative';c=w/'classes';c.mkdir(parents=True,exist_ok=True)
cp=[str((a.build_dir/'prominence-korean-compat-1.0.0.jar').resolve()),*json.loads(a.classpath.read_text('utf8'))]
def run(cmd,name):
 q=subprocess.run(list(map(str,cmd)),capture_output=True,text=True,encoding='utf8',errors='replace');(w/name).write_text(q.stdout+q.stderr,'utf8');assert q.returncode==0,q.stdout+q.stderr;print(q.stdout[-1800:])
j=a.java_home/'bin'
run([j/'javac.exe','--release','17','-encoding','UTF-8','-proc:none','-cp',os.pathsep.join(cp),'-d',c,r/'tests/native/batchverification/VerifyMixinTransforms.java',r/'tests/native/workverification/VerifyCreativeTransforms.java',r/'tests/FacadeBuildScopeTests.java',r/'tests/JoinGuardTests.java'],'compile.log')
s=c/'META-INF/services';s.mkdir(parents=True,exist_ok=True)
for key,value in {'org.spongepowered.asm.service.IMixinService':'batchverification.VerifyMixinTransforms','org.spongepowered.asm.service.IMixinServiceBootstrap':'batchverification.VerifyMixinTransforms$Bootstrap','org.spongepowered.asm.service.IGlobalPropertyService':'batchverification.VerifyMixinTransforms$Props'}.items():(s/key).write_text(value+'\n','utf8')
src=w/'fixture-src';fc=w/'fixture-classes';fc.mkdir(exist_ok=True)
sources={
'net/minecraft/class_1792.java':'''package net.minecraft; public class class_1792 { public final String id; public class_1792(String id){this.id=id;} }''',
'net/minecraft/class_1747.java':'''package net.minecraft; public class class_1747 extends class_1792 {public class_1747(String id){super(id);} }''',
'net/minecraft/class_2487.java':'''package net.minecraft; public record class_2487(String item) {}''',
'net/minecraft/class_1799.java':'''package net.minecraft;
public class class_1799 {
 public static final class_1799 field_8037=new class_1799(null,null); public final class_1792 item;public final class_2487 nbt;
 public class_1799(class_1792 item,class_2487 nbt){this.item=item;this.nbt=nbt;}
 public boolean method_7960(){return item==null;} public boolean method_7985(){return nbt!=null;}
 public class_1792 method_7909(){return item;} public class_2487 method_7969(){return nbt;}
 public static boolean method_31577(class_1799 a,class_1799 b){return a.item==b.item&&java.util.Objects.equals(a.nbt,b.nbt);}
 public String toString(){return item==null?"empty":item.id+":"+nbt;}
}''',
'net/minecraft/class_1761.java':'''package net.minecraft; import java.util.*; public class class_1761 {
 public final Collection<class_1799> contents; public class_1761(Collection<class_1799> c){contents=c;}
 public Collection<class_1799> method_47313(){return contents;}
 public static class class_8128 {} public interface class_7704 {void method_45423(Collection<class_1799> c);}
}''',
'net/minecraft/class_7706.java':'''package net.minecraft; import java.util.*; public class class_7706 {
 public static List<class_1761> tabs;public static List<class_1761> method_47341(){return tabs;}
}''',
'appeng/core/definitions/ItemDefinition.java':'''package appeng.core.definitions; import net.minecraft.*;public class ItemDefinition {
 public final class_1792 item; public ItemDefinition(class_1792 i){item=i;}public class_1792 method_8389(){return item;}
}''',
'appeng/core/definitions/AEItems.java':'''package appeng.core.definitions; public class AEItems {public static ItemDefinition FACADE;}''',
'appeng/items/parts/FacadeItem.java':'''package appeng.items.parts; import java.util.*;import net.minecraft.*; public class FacadeItem extends class_1792 {
 public int calls; public final Set<String> blocked=new HashSet<>();public FacadeItem(){super("ae2:facade");}
 public class_1799 createFacadeForItem(class_1799 s,boolean test){calls++;if(s.method_7960()||s.method_7985()||!(s.item instanceof class_1747))return class_1799.field_8037;
  if(s.item.id.equals("throws"))throw new IllegalArgumentException("native catch probe");if(blocked.contains(s.item.id))return class_1799.field_8037;
  return test?s:new class_1799(this,new class_2487(s.item.id));}
}''',
'fixture/CreativeFixture.java':'''package fixture;import java.util.*;import java.lang.reflect.*;import net.minecraft.*;import appeng.items.parts.*;import appeng.core.definitions.*;
public class CreativeFixture {
 public static int firstCalls; public static class CustomBlock extends class_1747 {CustomBlock(String id){super(id);}}
 public static class CustomStack extends class_1799 {CustomStack(class_1792 i){super(i,null);}}
 static List<String> invoke(FacadeItem factory,List<class_1761> tabs)throws Exception {
  AEItems.FACADE=new ItemDefinition(factory);class_7706.tabs=tabs;
  var clazz=Class.forName("appeng.core.FacadeCreativeTab");var field=clazz.getDeclaredField("group");field.setAccessible(true);
  var self=new class_1761(List.of(new class_1799(new class_1747("self-must-be-skipped"),null)));field.set(null,self);tabs.add(self);
  var method=clazz.getDeclaredMethod("buildDisplayItems",class_1761.class_8128.class,class_1761.class_7704.class);method.setAccessible(true);
  List<String> result=new ArrayList<>();class_1761.class_7704 output=values->{for(var v:values)result.add(v.toString());};
  try{method.invoke(null,new class_1761.class_8128(),output);}finally{tabs.remove(self);}return result;
 }
 public static List<String> run(boolean fixed)throws Exception {
  var f=new FacadeItem();var a=new class_1747("stone");var b=new class_1747("dirt");var sameId=new class_1747("stone");var custom=new CustomBlock("custom");
  List<class_1799> entries=new ArrayList<>();for(int i=0;i<1000;i++){entries.add(new class_1799(a,null));entries.add(new class_1799(b,null));}
  entries.add(new class_1799(a,new class_2487("data")));entries.add(new class_1799(a,null));entries.add(new CustomStack(a));entries.add(new CustomStack(a));
  entries.add(new class_1799(custom,null));entries.add(new class_1799(custom,null));entries.add(new class_1799(new class_1792("not-a-block"),null));entries.add(class_1799.field_8037);entries.add(new class_1799(sameId,null));
  var tabs=new ArrayList<class_1761>();tabs.add(new class_1761(entries));tabs.add(new class_1761(List.of(new class_1799(b,null))));
  var result=invoke(f,tabs);firstCalls=f.calls;if(fixed&&firstCalls!=10)throw new AssertionError("Expected exact per-build native calls: "+firstCalls);
  f.blocked.add("stone");int before=f.calls;var changed=invoke(f,tabs);if(changed.stream().anyMatch(x->x.contains("item=stone")))throw new AssertionError("Rebuild kept stale tags/config");if(fixed&&f.calls-before!=10)throw new AssertionError("Rebuild calls changed");
  var failTabs=new ArrayList<class_1761>();failTabs.add(new class_1761(List.of(new class_1799(b,null),new class_1799(new class_1747("throws"),null),new class_1799(a,null))));
  var partial=invoke(f,failTabs);if(partial.size()!=1||!partial.get(0).contains("dirt"))throw new AssertionError("Native Throwable catch/output changed");
  if(invoke(f,tabs).isEmpty())throw new AssertionError("Scope was not cleared after native catch");return result;
 }
}'''
}
paths=[]
for name,body in sources.items():
 path=src/name;path.parent.mkdir(parents=True,exist_ok=True);path.write_text(body,'utf8');paths.append(path)
run([j/'javac.exe','--release','17','-encoding','UTF-8','-d',fc,*paths],'fixture-compile.log')
base=[j/'java.exe','-Xmx512m','-ea','-Djava.awt.headless=true','-cp',os.pathsep.join([str(c),*cp])]
run([*base,'local.prominence.work.FacadeBuildScopeTests'],'helpers.log')
run([*base,'local.prominence.join.JoinGuardTests',r/'src/main/resources/prominence-emi-home-pins.json'],'guards.log')
run([*base,'-Dmixin.service=batchverification.VerifyMixinTransforms','-Dmixin.bootstrapService=batchverification.VerifyMixinTransforms$Bootstrap','workverification.VerifyCreativeTransforms',r/'src/main/resources/prominence-emi-home.client.mixins.json',w/'verification',fc],'transform.log')
