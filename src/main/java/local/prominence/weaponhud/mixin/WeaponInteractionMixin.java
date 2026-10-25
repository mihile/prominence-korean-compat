package local.prominence.weaponhud.mixin;

import local.prominence.weaponhud.WeaponAbilityClient;
import net.minecraft.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value=class_636.class,priority=500)
public abstract class WeaponInteractionMixin {
    @Inject(method="method_2919",at=@At("HEAD"),cancellable=true)
    private void prominence$weaponUse(class_1657 player,class_1268 hand,CallbackInfoReturnable<class_1269> cir){
        // Before the vanilla item-use packet is sent; block/entity interaction methods remain untouched.
        if(WeaponAbilityClient.blockedDefault(player,hand))cir.setReturnValue(class_1269.field_5811);
    }
    @Inject(method={"method_2896","method_2905","method_2917"},at=@At("RETURN"))
    private void prominence$interactionResult(CallbackInfoReturnable<class_1269> cir){WeaponAbilityClient.interacted(cir.getReturnValue());}
}
