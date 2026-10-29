package local.prominence.work;
import java.math.*;
public final class LootNumbers {
 private LootNumbers(){}
 public static String precise(float value){if(!Float.isFinite(value))return Float.toString(value);return new BigDecimal(Float.toString(value)).round(new MathContext(6,RoundingMode.HALF_UP)).stripTrailingZeros().toPlainString();}
 public static String number(double value){if(!Double.isFinite(value))return "?";return BigDecimal.valueOf(value).round(new MathContext(6,RoundingMode.HALF_UP)).stripTrailingZeros().toPlainString();}
 public static String rolls(double value,boolean ko){if(value<0||!Double.isFinite(value))return ko?"평균 선택 횟수: 알 수 없음":"Average selections: unknown";
  return ko?"조건 충족 시 평균 선택: "+number(value)+"회":"Average selections after conditions: "+number(value);
 }
}
