package local.prominence.weaponhud.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import local.prominence.weaponhud.WeaponAbilityClient;
import net.minecraft.class_1268;
import net.minecraft.class_304;
import net.sweenus.simplyswords.client.AbilityKeybindHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value=AbilityKeybindHandler.class,remap=false)
public abstract class AbilityBindingMixin {
    @ModifyExpressionValue(method="tickHand",at=@At(value="INVOKE",target="Lnet/minecraft/class_304;method_1434()Z"))
    private static boolean prominence$contextualPress(boolean pressed,class_1268 hand,class_304 binding){return pressed&&!WeaponAbilityClient.blocked(binding);}
}
