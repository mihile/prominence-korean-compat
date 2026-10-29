package local.prominence.rewards.mixin;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;import local.prominence.rewards.RewardScope;import net.minecraft.class_85;import org.spongepowered.asm.mixin.*;import org.spongepowered.asm.mixin.injection.At;
@Mixin(targets="net.minecraft.class_85$class_88")
public abstract class RewardWeightMixin {
 @Shadow @Final private class_85 field_1004;
 @ModifyReturnValue(method="method_427",at=@At("RETURN"))
 private int prominence$weight(int original){return RewardScope.weight(field_1004,original);}
}
