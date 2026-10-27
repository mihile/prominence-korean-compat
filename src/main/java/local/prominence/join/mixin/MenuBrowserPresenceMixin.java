package local.prominence.join.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Classpath presence is stable; the browser's critical-failure state remains live. */
@Mixin(targets="de.keksuccino.fancymenu.util.mcef.MCEFUtil", remap=false)
public abstract class MenuBrowserPresenceMixin {
    @Shadow public static volatile boolean MCEF_critical_failure;
    @Unique private static volatile Boolean prominence$browserClassPresent;
    @Inject(method="isMCEFLoaded", at=@At("HEAD"), cancellable=true, remap=false, require=1)
    private static void prominence$reuseClassPresence(CallbackInfoReturnable<Boolean> ci) {
        Boolean present=prominence$browserClassPresent;
        if (MCEF_critical_failure) ci.setReturnValue(false);
        else if (present!=null) ci.setReturnValue(present);
    }
    @Inject(method="isMCEFLoaded", at=@At("RETURN"), remap=false, require=1)
    private static void prominence$rememberClassPresence(CallbackInfoReturnable<Boolean> ci) {
        // Do not cache a transient critical failure as absent. Also avoid caching arbitrary failures.
        // The guard enables this optimization only when MCEF is absent from the startup classpath.
        if (!MCEF_critical_failure && !ci.getReturnValue()) prominence$browserClassPresent=false;
    }
}
