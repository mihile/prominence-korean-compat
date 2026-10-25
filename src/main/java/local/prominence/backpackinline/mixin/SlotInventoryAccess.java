package local.prominence.backpackinline.mixin;
import net.minecraft.class_1263;
import net.minecraft.class_1735;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(class_1735.class)
public interface SlotInventoryAccess {
    @Mutable @Accessor("field_7871") void prominence$inventory(class_1263 inventory);
}
