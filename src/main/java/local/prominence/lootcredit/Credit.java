package local.prominence.lootcredit;
import java.util.UUID;
import net.minecraft.*;
public record Credit(UUID owner,int looting,int scavenger) {
 public static Credit capture(class_1297 entity,class_1799 stack) {
  if(!(entity instanceof class_1657)||entity.method_37908().field_9236)return null;
  var scav=class_7923.field_41176.method_10223(new class_2960("zenith","scavenger"));
  return new Credit(entity.method_5667(),class_1890.method_8225(class_1893.field_9110,stack),scav==null?0:class_1890.method_8225(scav,stack));
 }
 public static Credit capture(class_1297 entity){return entity instanceof class_1309 l?capture(entity,l.method_6047()):null;}
 public class_1657 player(class_1937 world){return world.method_18470(owner);}
 public void write(class_2487 tag){var n=new class_2487();n.method_25927("Owner",owner);n.method_10569("Looting",looting);n.method_10569("Scavenger",scavenger);tag.method_10566("ProminenceWeaponLoot",n);}
 public static Credit read(class_2487 tag){if(!tag.method_10545("ProminenceWeaponLoot"))return null;var n=tag.method_10562("ProminenceWeaponLoot");if(!n.method_25928("Owner"))return null;return new Credit(n.method_25926("Owner"),Math.max(0,Math.min(32767,n.method_10550("Looting"))),Math.max(0,Math.min(32767,n.method_10550("Scavenger"))));}
}
