package local.prominence.work;
import java.util.*;
import java.util.zip.*;
import java.nio.file.*;
public final class TrimPreviewAssetsTests {
 public static void main(String[] args)throws Exception {
  Set<String> files=new HashSet<>();
  if(args.length>0)try(var z=new ZipFile(args[0])){var es=z.entries();while(es.hasMoreElements()){String s=es.nextElement().getName();if(s.startsWith("assets/minecraft/"))files.add("minecraft:"+s.substring("assets/minecraft/".length()));}}
  else for(String name:List.of("beast","fever","greed","myth","ram","storm"))for(String slot:List.of("helmet","chestplate","leggings","boots"))files.add("minecraft:textures/trims/items/"+slot+"/more_armor_trims-"+name+"_armor_trim_smithing_template.png");
  for(String name:List.of("beast","fever","greed","myth","ram","storm"))if(!TrimPreviewAssets.hasMasks("more_armor_trims:"+name+"_armor_trim_smithing_template",files::contains))throw new AssertionError("Supported pattern lost: "+name);
  for(String name:List.of("horizon","nihility","origin","parasite","twilight","witness","wraith"))if(TrimPreviewAssets.hasMasks("more_armor_trims:"+name+"_armor_trim_smithing_template",files::contains))throw new AssertionError("Missing pattern incorrectly accepted: "+name);
  String added="more_armor_trims:horizon_armor_trim_smithing_template";
  if(!TrimPreviewAssets.hasMasks(added,p->true))throw new AssertionError("Resource-pack provided masks rejected");
  if(TrimPreviewAssets.hasMasks(added,p->!p.contains("/boots/")))throw new AssertionError("Partial set accepted");
  var ids=List.of(new net.minecraft.class_2960("more_armor_trims:beast_armor_trim_smithing_template"),new net.minecraft.class_2960(added));
  final boolean[] all={false};final int[] queries={0};
  var manager=(net.minecraft.class_3300)java.lang.reflect.Proxy.newProxyInstance(TrimPreviewAssetsTests.class.getClassLoader(),new Class<?>[]{net.minecraft.class_3300.class},(proxy,method,arguments)->{
   if(method.getName().equals("method_14486")){queries[0]++;boolean present=all[0]||files.contains(arguments[0].toString());return present?Optional.of(new net.minecraft.class_3298(null,()->new java.io.ByteArrayInputStream(new byte[0]))):Optional.empty();}
   throw new UnsupportedOperationException(method.getName());
  });
  TrimPreviewAssets.beginReload(manager);var first=TrimPreviewAssets.supported(ids);
  if(first.size()!=1||ids.size()!=2)throw new AssertionError("Source IDs mutated or missing pattern kept");
  int before=queries[0];if(TrimPreviewAssets.supported(ids)!=first||queries[0]!=before)throw new AssertionError("Repeated model checks re-read resources");
  all[0]=true;TrimPreviewAssets.beginReload(manager);
  if(TrimPreviewAssets.supported(ids).size()!=2)throw new AssertionError("Reload did not restore resource-pack supplied preview");
  System.out.println("PASS: six supported patterns, seven missing patterns, resource-pack additions and partial-mask fallback");
 }
}
