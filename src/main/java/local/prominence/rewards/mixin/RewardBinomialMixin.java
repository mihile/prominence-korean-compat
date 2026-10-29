package local.prominence.rewards.mixin;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;import local.prominence.rewards.RewardScope;import net.minecraft.*;import org.spongepowered.asm.mixin.Mixin;import org.spongepowered.asm.mixin.injection.At;
@Mixin(class_40.class)
public abstract class RewardBinomialMixin {
 @ModifyExpressionValue(method="method_366",at=@At(value="INVOKE",target="Lnet/minecraft/class_5658;method_32454(Lnet/minecraft/class_47;)F"))
 private float prominence$bonus(float chance,class_47 context){return RewardScope.probability(this,context,chance);}
}
