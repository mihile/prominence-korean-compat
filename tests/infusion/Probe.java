package infusionfixture;
import java.util.*;
import net.minecraft.*;
import elocindev.eldritch_end.api.infusion.*;
import local.prominence.infusion.InfusionReplacement;

public final class Probe {
 static int checks;
 static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
 static class Template extends class_1792 implements InfusionTemplate {
  boolean allowed=true;public boolean isEquipmentAllowed(class_1799 s){return allowed;}
 }
 static class Material extends class_1792 implements InfusableItemMaterial {
  Template template;class_1320 attribute;double amount;boolean enabled=true;
  Material(Template t,class_1320 a,double v){template=t;attribute=a;amount=v;}
  public List<InfusionAttributeHolder> getInfusionAttributes(){return List.of(new InfusionAttributeHolder(attribute,amount));}
  public class_1792 getInfusionTemplate(){return template;}
  public boolean applyToArmor(){return enabled;}public boolean isInfusable(){return enabled;}
 }
 static class_2487 modifier(String attr,String name,String slot,double amount){
  var e=new class_2487();e.method_10582("AttributeName",attr);e.method_10582("Name",name);e.method_10582("Slot",slot);e.method_10549("Amount",amount);return e;
 }
 static class_1799 armor(String from,String attr,double amount){
  var s=new class_1799(new class_1738());s.nbt=new class_2487();
  s.nbt.method_10582("display","custom-name");s.nbt.method_10582("Enchantments","original-enchantments");s.nbt.method_10569("Damage",29);
  s.nbt.method_10582("tiered","original-reforge");s.nbt.method_10582("prominence_artifact","original-progression");
  var i=s.method_7911("eldritch_infusions");i.method_10556("isInfused",true);i.method_10582("materialIdentifier",from);i.method_10582("currentInfusion",from.split(":")[1]);i.method_10582("extra","preserved");
  var modifiers=new class_2499();modifiers.add(modifier(attr,"Infusion modifier","chest",amount));
  modifiers.add(modifier("minecraft:generic.armor","Armor modifier","chest",8));
  modifiers.add(modifier(attr,"Another upgrade","chest",7));
  modifiers.add(modifier(attr,"Infusion modifier","head",3));
  s.nbt.method_10566("AttributeModifiers",modifiers);return s;
 }
 public static void main(String[] args){
  var corruption=new class_1320();var resist=new class_1320();
  String ca="eldritch_end:corruption",ra="eldritch_end:corruption_resistance";
  class_7923.field_41190.values.put(new class_2960(ca),corruption);class_7923.field_41190.values.put(new class_2960(ra),resist);
  var corruptionTemplate=new Template();var etyrTemplate=new Template();
  var materials=new LinkedHashMap<String,Material>();
  materials.put("eldritch_end:aberration_limb",new Material(corruptionTemplate,corruption,5));
  materials.put("eldritch_end:aberration_heart",new Material(corruptionTemplate,corruption,15));
  materials.put("eldritch_end:xal",new Material(corruptionTemplate,corruption,25));
  materials.put("eldritch_end:etyr_ingot",new Material(etyrTemplate,resist,10));
  materials.forEach((id,m)->class_7923.field_41178.values.put(new class_2960(id),m));
  for(var from:materials.entrySet())for(var to:materials.entrySet()) {
   var original=armor(from.getKey(),from.getValue().attribute==corruption?ca:ra,from.getValue().amount);var before=original.nbt.method_10553();
   var result=InfusionReplacement.preview(new class_1799(to.getValue().template),original,new class_1799(to.getValue()));
   check(original.nbt.equals(before),"preview mutated input");
   if(from.getKey().equals(to.getKey())){check(result==null,"same infusion consumes materials");continue;}
   check(result!=null,"missing replacement "+from.getKey()+" -> "+to.getKey());
   for(String key:List.of("display","Enchantments","Damage","tiered","prominence_artifact"))check(Objects.equals(result.nbt.method_10580(key),before.method_10580(key)),"lost "+key);
   var list=result.nbt.method_10554("AttributeModifiers",10);check(list.size()==4,"modifier accumulation");
   for(int i=0;i<3;i++)check(list.method_10602(i).equals(before.method_10554("AttributeModifiers",10).method_10602(i+1)),"lost unrelated modifier");
   check(list.method_10602(3).method_10574("Amount")==to.getValue().amount,"wrong new infusion amount");
   check(result.method_7911("eldritch_infusions").method_10558("materialIdentifier").equals(to.getKey()),"wrong metadata");
   check(result.method_7911("eldritch_infusions").method_10558("extra").equals("preserved"),"lost extra metadata");
  }
  var heart=materials.get("eldritch_end:aberration_heart");var etyr=materials.get("eldritch_end:etyr_ingot");
  var armor=armor("eldritch_end:aberration_heart",ca,15);var add=new class_1799(etyr);var tpl=new class_1799(etyrTemplate);
  check(InfusionReplacement.preview(new class_1799(corruptionTemplate),armor,add)==null,"wrong template accepted");
  etyr.enabled=false;check(InfusionReplacement.preview(tpl,armor,add)==null,"disabled config bypass");etyr.enabled=true;
  etyrTemplate.allowed=false;check(InfusionReplacement.preview(tpl,armor,add)==null,"template restriction bypass");etyrTemplate.allowed=true;
  armor.method_7911("eldritch_infusions").method_10582("materialIdentifier","other:unknown");check(InfusionReplacement.preview(tpl,armor,add)==null,"unknown infusion modified");
  armor=armor("eldritch_end:aberration_heart",ca,15);armor.nbt.method_10551("AttributeModifiers");check(InfusionReplacement.preview(tpl,armor,add)==null,"missing ownership accepted");
  var empty=new class_1799(null);check(InfusionReplacement.preview(tpl,armor,empty)==null,"empty addition");
  check(InfusionReplacement.preview(tpl,new class_1799(new class_1738()),add)==null,"first infusion intercepted");
  System.out.println("Infusion replacement fixture passed: "+checks+" checks using native NBT; no game/world started");
 }
}
