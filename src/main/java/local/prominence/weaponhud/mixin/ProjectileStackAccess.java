package local.prominence.weaponhud.mixin;
import net.minecraft.class_1799;
import net.minecraft.class_2940;
import net.sweenus.simplyswords.entity.ThrownSwordEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(value=ThrownSwordEntity.class,remap=false)
public interface ProjectileStackAccess {
    @Accessor("ITEM_STACK") static class_2940<class_1799> prominence$stackKey(){throw new AssertionError();}
}
