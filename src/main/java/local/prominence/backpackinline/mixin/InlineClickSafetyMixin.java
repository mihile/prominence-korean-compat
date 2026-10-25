package local.prominence.backpackinline.mixin;

import local.prominence.backpackinline.InlineReforge;
import net.minecraft.class_1657;
import net.minecraft.class_1703;
import net.minecraft.class_1713;
import net.minecraft.class_1735;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(class_1703.class)
public abstract class InlineClickSafetyMixin {
    @Inject(method="method_7593",at=@At("HEAD"),cancellable=true)
    private void prominence$click(int id,int button,class_1713 type,class_1657 player,CallbackInfo ci){if(InlineReforge.blocked(this,id))ci.cancel();}
    @Inject(method="method_7615",at=@At("HEAD"),cancellable=true)
    private void prominence$drag(class_1735 slot,CallbackInfoReturnable<Boolean> cir){if(InlineReforge.blocked(this,slot))cir.setReturnValue(false);}
}
