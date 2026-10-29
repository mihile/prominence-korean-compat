package local.prominence.lootcredit.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import local.prominence.lootcredit.*;
import net.minecraft.*;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(class_1309.class)
public abstract class LivingCreditMixin {
 @WrapMethod(method="method_5643") private boolean credit$damage(class_1282 source,float amount,Operation<Boolean> original){
  var target=(class_1309)(Object)this;var credit=LootCredit.damage(target,source);
  try(var scope=CreditScope.enter(credit,false)){return original.call(source,amount);}
 }
 @WrapMethod(method="method_16080") private void credit$drops(class_1282 source,Operation<Void> original){var t=(class_1309)(Object)this;var c=CreditScope.current();original.call(LootCredit.attributed(t,source,c==null?null:c.credit()));}
 @com.llamalad7.mixinextras.injector.ModifyExpressionValue(method="method_16080",at=@org.spongepowered.asm.mixin.injection.At(value="FIELD",target="Lnet/minecraft/class_1309;field_6238:I"))
 private int credit$recent(int original){var c=CreditScope.current();return c!=null&&c.credit()!=null&&c.credit().player(((class_1309)(Object)this).method_37908())!=null?Math.max(1,original):original;}
 @com.llamalad7.mixinextras.injector.ModifyExpressionValue(method="method_16077",at=@org.spongepowered.asm.mixin.injection.At(value="FIELD",target="Lnet/minecraft/class_1309;field_6258:Lnet/minecraft/class_1657;"))
 private class_1657 credit$last(class_1657 original){var c=CreditScope.current();return c!=null&&c.credit()!=null?c.credit().player(((class_1309)(Object)this).method_37908()):original;}
 @WrapMethod(method="method_37222") private boolean credit$effect(class_1293 effect,class_1297 source,Operation<Boolean> original){
  var scope=CreditScope.current();Credit c=source instanceof class_1657?LootCredit.current(source):scope==null?null:scope.credit();
  if(LootCredit.weaponEffect(effect))((CreditHolder)effect).prominence$credit(c);
  boolean result=original.call(effect,source);
  if(result&&LootCredit.weaponEffect(effect)) {var actual=((class_1309)(Object)this).method_6112(effect.method_5579());if(actual instanceof CreditHolder h)h.prominence$credit(c);}
  return result;
 }
}
