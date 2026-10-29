package local.prominence.work;
import java.math.*;
public final class LootNumbers {
 private LootNumbers(){}
 public static String percent(double value){if(!Double.isFinite(value)||value<0)return "?";if(value>0&&value<.01)return "<0.01";return BigDecimal.valueOf(value).setScale(2,RoundingMode.DOWN).stripTrailingZeros().toPlainString();}
 public static String precise(float value){if(!Float.isFinite(value))return Float.toString(value);return new BigDecimal(Float.toString(value)).round(new MathContext(6,RoundingMode.HALF_UP)).stripTrailingZeros().toPlainString();}
 public static String number(double value){if(!Double.isFinite(value))return "?";return BigDecimal.valueOf(value).round(new MathContext(6,RoundingMode.HALF_UP)).stripTrailingZeros().toPlainString();}
}
