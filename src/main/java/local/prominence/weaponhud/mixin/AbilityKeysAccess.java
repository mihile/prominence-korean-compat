package local.prominence.weaponhud.mixin;

import net.minecraft.class_304;
import net.sweenus.simplyswords.client.AbilityKeybindHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value=AbilityKeybindHandler.class,remap=false)
public interface AbilityKeysAccess {
    @Accessor("MAINHAND_ABILITY") static class_304 prominence$main(){throw new AssertionError();}
    @Accessor("OFFHAND_ABILITY") static class_304 prominence$off(){throw new AssertionError();}
}
