package local.prominence.infusion.mixin;

import local.prominence.infusion.InfusionReplacement;
import net.minecraft.class_4862;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=class_4862.class,priority=900,remap=false)
public abstract class SmithingInfusionMixin {
    @Inject(method="method_24928",at=@At("HEAD"),cancellable=true,require=1)
    private void prominence$replaceInfusion(CallbackInfo ci) {
        var handler=(class_4862)(Object)this;
        if(handler.field_7761.size()<4)return;
        var result=InfusionReplacement.preview(handler.method_7611(0).method_7677(),
            handler.method_7611(1).method_7677(),handler.method_7611(2).method_7677());
        if(result==null)return;
        handler.method_7611(3).method_7673(result);
        ci.cancel();
    }
}
