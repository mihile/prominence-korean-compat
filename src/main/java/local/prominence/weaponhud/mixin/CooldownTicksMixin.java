package local.prominence.weaponhud.mixin;
import java.util.Map;
import local.prominence.weaponhud.CooldownTicks;
import net.minecraft.class_1792;
import net.minecraft.class_1796;
import org.spongepowered.asm.mixin.*;
@Mixin(class_1796.class)
public abstract class CooldownTicksMixin implements CooldownTicks {
    @Shadow @Final private Map<class_1792,?> field_8024;
    @Shadow private int field_8025;
    public int prominence$remaining(class_1792 item){var entry=field_8024.get(item);return entry==null?0:Math.max(0,((CooldownEntryAccess)entry).prominence$end()-field_8025);}
}
