package local.prominence.rewards.client;
import dev.emi.emi.api.widget.SlotWidget;import net.minecraft.*;import java.util.List;import org.spongepowered.asm.mixin.*;import org.spongepowered.asm.mixin.injection.*;import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=SlotWidget.class,remap=false)
public abstract class RewardSlotMixin implements RewardSlot {
 @Unique private RewardClientText.TooltipSource prominence$source;
 public void prominence$source(RewardClientText.TooltipSource source){prominence$source=source;}
 @Inject(method="addSlotTooltip",at=@At("HEAD"))
 private void prominence$liveChance(List<class_5684> components,CallbackInfo ci){if(prominence$source!=null)for(var line:RewardClientText.current(prominence$source))components.add(class_5684.method_32662(line.method_30937()));}
}
