package local.prominence.ftbime.mixin;
import dev.ftb.mods.ftblibrary.ui.Widget;
import local.prominence.ftbime.ImeTarget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=Widget.class,remap=false)
public abstract class FtbWidgetCloseMixin {
    @Inject(method="onClosed",at=@At("HEAD"))
    private void prominence$releaseIme(CallbackInfo ci){if(this instanceof ImeTarget target)target.prominence$imeClosed();}
}
