package local.prominence.rewards.mixin;
import local.prominence.rewards.RewardLeaf;import net.minecraft.class_85;import org.spongepowered.asm.mixin.Mixin;import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(class_85.class)
public interface RewardLeafMixin extends RewardLeaf {
 @Accessor("field_994") int prominence$quality();
}
