package local.prominence.lootcredit;
import net.minecraft.*;
public final class LootCredit {
 private LootCredit(){}
 public static Credit projectile(class_1297 source){return source instanceof CreditHolder h?h.prominence$credit():null;}
 public static Credit current(class_1297 actor){var c=CreditScope.current();return c!=null&&c.credit()!=null&&actor!=null&&c.credit().owner().equals(actor.method_5667())?c.credit():Credit.capture(actor);}
 public static Credit damage(class_1309 target,class_1282 source){
  if(target.method_37908().field_9236)return null;
  var actor=source.method_5529();var credit=source instanceof CreditHolder h?h.prominence$credit():null;
  if(credit==null)credit=projectile(source.method_5526());
  if(credit!=null&&actor!=null&&credit.owner().equals(actor.method_5667()))return credit;
  if(actor instanceof class_1657 p){if(source.getClass().getName().equals("chronosacaria.mcdw.damagesources.OffHandDamageSource"))return Credit.capture(p,p.method_6079());return current(actor);}
  var scope=CreditScope.current();
  if(actor==null&&scope!=null&&scope.anonymous()&&scope.credit()!=null&&!scope.credit().owner().equals(target.method_5667())&&scope.credit().player(target.method_37908())!=null)return scope.credit();
  return null;
 }
 public static class_1282 attributed(class_1309 target,class_1282 source,Credit credit){
  if(credit==null||source.method_5529()!=null)return source;
  var player=credit.player(target.method_37908());if(player==null)return source;
  return new class_1282(source.method_48793(),source.method_5526(),player);
 }
 public static Credit dropCredit(class_1297 actor){var s=CreditScope.current();return s!=null&&s.credit()!=null&&actor!=null&&s.credit().owner().equals(actor.method_5667())?s.credit():null;}
 public static boolean weaponEffect(class_1293 e){String n=e.method_5579().getClass().getName();var id=class_7923.field_41174.method_10221(e.method_5579());if(id!=null&&(id.toString().equals("minecraft:poison")||id.toString().equals("minecraft:wither")))return true;return n.startsWith("net.sweenus.simplyswords.effect.")||n.startsWith("net.soulsweaponry.entity.effect.")||n.startsWith("chronosacaria.mcdw.effects.");}
}
