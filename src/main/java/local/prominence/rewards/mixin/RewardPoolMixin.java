package local.prominence.rewards.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import local.prominence.rewards.*;import net.minecraft.*;import org.spongepowered.asm.mixin.*;
import java.util.function.Consumer;
@Mixin(class_55.class)
public abstract class RewardPoolMixin implements RewardPool {
 @Shadow @Final private class_5658 field_958;
 @Unique private long prominence$rewardEpoch=Long.MIN_VALUE;
 @Unique private RewardPlan prominence$reward;
 public RewardPlan prominence$rewardPlan(){long epoch=RewardRegistry.epoch();if(prominence$rewardEpoch!=epoch){prominence$reward=RewardPlan.inspect((class_55)(Object)this,field_958);prominence$rewardEpoch=epoch;}return prominence$reward;}
 @WrapMethod(method="method_341")
 private void prominence$scaledDrop(Consumer<class_1799> output,class_47 context,Operation<Void> original){
  var plan=prominence$rewardPlan();if(plan==null||!plan.eligible()){original.call(output,context);return;}
  try(var scope=RewardScope.enter(plan,context)){original.call(output,context);}
 }
}
