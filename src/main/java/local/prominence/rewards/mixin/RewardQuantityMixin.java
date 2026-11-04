package local.prominence.rewards.mixin;
import local.prominence.rewards.RewardQuantity;
import net.minecraft.*;
import org.spongepowered.asm.mixin.*;
@Mixin(class_125.class)
public abstract class RewardQuantityMixin implements RewardQuantity {
    @Shadow @Final private class_5658 field_1082;
    @Shadow @Final private int field_1083;
    public class_5658 prominence$count(){return field_1082;}
    public int prominence$limit(){return field_1083;}
}
