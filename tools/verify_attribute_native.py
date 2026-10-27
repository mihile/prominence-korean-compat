from pathlib import Path
import argparse,json,subprocess,os
p=argparse.ArgumentParser();p.add_argument('--build-dir',type=Path,required=True);p.add_argument('--classpath',type=Path,required=True);p.add_argument('--java-home',type=Path,required=True);a=p.parse_args()
r=Path(__file__).resolve().parents[1];w=a.build_dir/'native-attribute';c=w/'classes';c.mkdir(parents=True,exist_ok=True);j=a.java_home/'bin'
cp=[str((a.build_dir/'prominence-korean-compat-1.0.0.jar').resolve()),*json.loads(a.classpath.read_text('utf8'))]
def run(cmd,name):
 q=subprocess.run(list(map(str,cmd)),capture_output=True,text=True,encoding='utf8',errors='replace');(w/name).write_text(q.stdout+q.stderr,'utf8');assert q.returncode==0,q.stdout+q.stderr;print(q.stdout[-1800:])
run([j/'javac.exe','--release','17','-encoding','UTF-8','-proc:none','-cp',os.pathsep.join(cp),'-d',c,r/'tests/native/batchverification/VerifyMixinTransforms.java',r/'tests/native/workverification/VerifyAttributeTransforms.java',r/'tests/AttributeCandidatesTests.java',r/'tests/JoinGuardTests.java'],'compile.log')
s=c/'META-INF/services';s.mkdir(parents=True,exist_ok=True)
for key,value in {'org.spongepowered.asm.service.IMixinService':'batchverification.VerifyMixinTransforms','org.spongepowered.asm.service.IMixinServiceBootstrap':'batchverification.VerifyMixinTransforms$Bootstrap','org.spongepowered.asm.service.IGlobalPropertyService':'batchverification.VerifyMixinTransforms$Props'}.items():(s/key).write_text(value+'\n','utf8')
sources={
'net/minecraft/class_1792.java':'package net.minecraft; public class class_1792 {public final String id;public class_1792(String id){this.id=id;}}',
'net/minecraft/class_2487.java':'package net.minecraft; public class class_2487 {public final java.util.Map<String,Boolean> values=new java.util.LinkedHashMap<>();public void method_10556(String key,boolean value){values.put(key,value);}public String toString(){return values.toString();}}',
'net/minecraft/class_1799.java':'''package net.minecraft;public class class_1799 {public final class_1792 item;public final class_2487 nbt=new class_2487();public class_1799(class_1792 item){this.item=item;}public class_1792 method_7909(){return item;}public class_2487 method_7969(){return nbt;}public class_2487 method_7953(class_2487 n){return n;} }''',
'net/minecraft/class_1304.java':'''package net.minecraft;public enum class_1304 {mainhand,offhand,head,chest,legs,feet;public static class_1304 method_5924(String id){return valueOf(id);}}''',
'net/minecraft/class_7922.java':'package net.minecraft;public interface class_7922 {class_2960 method_10221(Object item);Object method_10223(class_2960 id);}',
'net/minecraft/class_7923.java':'''package net.minecraft;public class class_7923 {public static final java.util.Map<class_2960,class_1792> items=new java.util.HashMap<>();public static final class_1792 air=new class_1792("minecraft:air");public static final class_7922 field_41178=new class_7922(){public class_2960 method_10221(Object item){return new class_2960(((class_1792)item).id);}public Object method_10223(class_2960 id){return items.getOrDefault(id,air);}};}''',
'elocindev/customitemattributes/config/CIAConfig.java':'package elocindev.customitemattributes.config;public class CIAConfig {public java.util.List<elocindev.customitemattributes.api.ItemProperty> items;}',
'elocindev/customitemattributes/CustomItemAttributes.java':'package elocindev.customitemattributes;public class CustomItemAttributes {public static elocindev.customitemattributes.config.CIAConfig CONFIG=new elocindev.customitemattributes.config.CIAConfig();public static org.slf4j.Logger LOGGER=org.slf4j.LoggerFactory.getLogger("fixture");}',
'elocindev/customitemattributes/api/ItemProperty.java':'''package elocindev.customitemattributes.api;import java.util.*;import net.minecraft.*;public class ItemProperty {public String item;public List<String> affected_slots;public List<String> attribute_overrides;public boolean unbreakable,force_unbreakable;public ItemProperty(String id){item=id;affected_slots=List.of("mainhand","offhand","head","chest","legs","feet");attribute_overrides=new ArrayList<>(List.of(id));}public class_1792 getItem(){return (class_1792)class_7923.field_41178.method_10223(new class_2960(item));}public List<String> getSlotNames(){return affected_slots;}public boolean shouldForceUnbreakable(){return force_unbreakable;}}''',
'fixture/AttributeFixture.java':'''package fixture;import java.util.*;import java.lang.reflect.*;import net.minecraft.*;import elocindev.customitemattributes.*;import elocindev.customitemattributes.api.*;import com.google.common.collect.*;
public class AttributeFixture {
 public static String run(boolean optimized)throws Exception {
  var source=new ArrayList<ItemProperty>();for(int i=0;i<100;i++){String id="example:item_"+i;source.add(new ItemProperty(id));class_7923.items.put(new class_2960(id),new class_1792(id));}
  var duplicate=new ItemProperty("example:item_10");duplicate.unbreakable=true;source.add(duplicate);CustomItemAttributes.CONFIG.items=source;
  var stack=new class_1799(class_7923.items.get(new class_2960("example:item_10")));var result=ArrayListMultimap.<Object,Object>create();
  var callback=Class.forName("elocindev.customitemattributes.builder.AttributeBuilderCallback").getDeclaredMethod("lambda$register$0",class_1799.class,class_1304.class,Multimap.class);callback.setAccessible(true);
  var old=Class.forName("local.prominence.emilookup.EmiItemLookupScope").getMethod("begin").invoke(null);
  var scope=optimized?Class.forName("local.prominence.work.AttributeCandidates").getMethod("begin").invoke(null):null;long requests;
  try {
   for(int i=0;i<100;i++)for(var slot:class_1304.values())callback.invoke(null,stack,slot,result);
   Field count=old.getClass().getDeclaredField("requests");count.setAccessible(true);requests=count.getLong(old);
   if(result.size()!=1200||!Boolean.TRUE.equals(stack.nbt.values.get("Unbreakable")))throw new AssertionError("Native modifiers/NBT output lost");
   // In-place item ID mutation, attribute mutation, and duplicate order must stay live.
   source.get(10).item="example:item_11";duplicate.attribute_overrides.add("changed");callback.invoke(null,stack,class_1304.mainhand,result);
   if(result.size()!=1201||!result.get("applied").toString().contains("changed"))throw new AssertionError("Mutable definition lost");
   source.add(new ItemProperty("INVALID ID"));int before=result.size();boolean failed=false;
   try{callback.invoke(null,stack,class_1304.mainhand,result);}catch(InvocationTargetException expected){failed=true;}
   if(!failed||result.size()!=before+1)throw new AssertionError("Malformed ID exception/native partial updates reordered");
   source.get(source.size()-1).item="example:item_11";callback.invoke(null,stack,class_1304.mainhand,result);
   Collections.reverse(source);callback.invoke(null,stack,class_1304.head,result);
   return "{\\"lookupScopeRequests\\":"+requests+",\\"outputCount\\":"+result.size()+",\\"outputDigest\\":\\""+java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(result.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)))+"\\",\\"nbt\\":\\""+stack.nbt+"\\"}";
  } finally {if(scope!=null)((AutoCloseable)scope).close();old.getClass().getMethod("close",boolean.class).invoke(old,true);}
 }
}'''
}
src=w/'fixture-src';fc=w/'fixture-classes';fc.mkdir(exist_ok=True);paths=[]
for name,body in sources.items():
 path=src/name;path.parent.mkdir(parents=True,exist_ok=True);path.write_text(body,'utf8');paths.append(path)
run([j/'javac.exe','--release','17','-encoding','UTF-8','-proc:none','-cp',os.pathsep.join(cp),'-d',fc,*paths],'fixture-compile.log')
base=[j/'java.exe','-Xmx512m','-ea','-Djava.awt.headless=true','-cp',os.pathsep.join([str(c),*cp])]
run([*base,'local.prominence.work.AttributeCandidatesTests'],'helpers.log')
run([*base,'local.prominence.join.JoinGuardTests',r/'src/main/resources/prominence-emi-home-pins.json'],'guards.log')
for mode in ['false','true']:run([*base,'-Dmixin.service=batchverification.VerifyMixinTransforms','-Dmixin.bootstrapService=batchverification.VerifyMixinTransforms$Bootstrap','workverification.VerifyAttributeTransforms',w/mode,mode,fc],mode+'.log')
old=json.loads((w/'false/native-report.json').read_text('utf8'));new=json.loads((w/'true/native-report.json').read_text('utf8'))
for field in ['outputCount','outputDigest','nbt']:assert old['fixture'][field]==new['fixture'][field],field
assert old['fixture']['lookupScopeRequests']==60600 and new['fixture']['lookupScopeRequests']==1200
report={'status':'passed','nativeCallbackOutputOrderNbtErrorsAndMutationEqual':True,'previousLookupCacheEnabledForBoth':True,'previousScopeRequests':60600,'optimizedScopeRequests':1200,'fixture':new,'gameStarted':False}
(w/'report.json').write_text(json.dumps(report,indent=2)+'\n','utf8');print(json.dumps(report,indent=2))
