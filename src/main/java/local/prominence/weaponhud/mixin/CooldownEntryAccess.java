package local.prominence.weaponhud.mixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(targets="net.minecraft.class_1796$class_1797")
public interface CooldownEntryAccess {@Accessor("field_8027") int prominence$end();}
