package local.prominence.backpackinline.mixin;
import elocindev.tierify.screen.ReforgeScreenHandler;
import net.minecraft.class_1263;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(value=ReforgeScreenHandler.class,remap=false)
public interface ReforgeInventoryAccess {
    @Mutable @Accessor("inventory") void prominence$inventory(class_1263 inventory);
    @Accessor("reforgeReady") boolean prominence$ready();
}
