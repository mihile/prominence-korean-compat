package local.prominence.work;
import java.math.*;
public final class LootNumbers {
 private LootNumbers(){}
 public static String precise(float value){if(!Float.isFinite(value))return Float.toString(value);return new BigDecimal(Float.toString(value)).round(new MathContext(6,RoundingMode.HALF_UP)).stripTrailingZeros().toPlainString();}
 public static String number(double value){if(!Double.isFinite(value))return "?";return BigDecimal.valueOf(value).round(new MathContext(6,RoundingMode.HALF_UP)).stripTrailingZeros().toPlainString();}
 public static String rolls(double value,boolean ko){if(value<0||!Double.isFinite(value))return ko?"추첨 기대치: 알 수 없음":"Expected selections: unknown";double percent=value*100;String pct=percent>0&&percent<0.000001?"<0.000001":number(percent);
  return ko?"추첨 기대치: "+pct+"% (평균 "+number(value)+"회)":"Expected selections: "+pct+"% ("+number(value)+" rolls on average)";
 }
}
