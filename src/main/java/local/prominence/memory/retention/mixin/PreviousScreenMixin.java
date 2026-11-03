package local.prominence.memory.retention.mixin;

import local.prominence.memory.*;
import net.minecraft.class_437;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets="dev.ftb.mods.ftblibrary.ui.BaseScreen",remap=false)
public abstract class PreviousScreenMixin implements RetainedClientOwner {
    @Mutable @Shadow @Final private class_437 prevScreen;
    @Inject(method="<init>",at=@At("RETURN"))
    private void prominence$track(CallbackInfo ci){RetentionCleanupClient.trackScreen(this);}
    public Object prominence$retainedOwner(){return prevScreen;}
    public void prominence$releaseOwner(){prevScreen=null;}
}
