package local.prominence.rewards.mixin;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;import local.prominence.rewards.RewardScope;import net.minecraft.*;import org.spongepowered.asm.mixin.Mixin;import org.spongepowered.asm.mixin.injection.At;
@Mixin(class_219.class)
public abstract class RewardChanceMixin {
 @ModifyExpressionValue(method="method_934",at=@At(value="FIELD",target="Lnet/minecraft/class_219;field_1296:F"))
 private float prominence$bonus(float chance,class_47 context){return RewardScope.probability(this,context,chance);}
}
