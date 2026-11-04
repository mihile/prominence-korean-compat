package local.prominence.work;
import net.minecraft.*;
/** Undo boss-bar font glyph substitutions only in EMI entity names. */
public final class PlainBossName {
 private PlainBossName() {}
 public static class_2561 get(class_2960 mob,class_2561 nativeName) {
  String text=nativeName.getString();boolean glyph=text.codePoints().anyMatch(c->c>=0xe000&&c<=0xf8ff);
  if(!glyph)return nativeName;
  return switch(mob.toString()) {
   case "minecraft:wither" -> class_2561.method_43471("prominence_korean_compat.plain_boss.wither");
   case "minecraft:ender_dragon" -> class_2561.method_43471("prominence_korean_compat.plain_boss.ender_dragon");
   default -> nativeName;
  };
 }
}
