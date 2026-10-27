package local.prominence.work.mixin;

import local.prominence.work.InitialWork;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets="me.jellysquid.mods.sodium.client.render.chunk.compile.executor.ChunkBuilder",remap=false)
public abstract class TerrainInitialBudgetMixin {
    @Shadow public abstract int getTotalThreadCount();
    @Shadow public abstract int getBusyThreadCount();
    @Shadow public abstract int getScheduledJobCount();
    @Inject(method="getSchedulingBudget",at=@At("RETURN"),cancellable=true,remap=false,require=1)
    private void prominence$leaveRoomDuringEmi(CallbackInfoReturnable<Integer> ci) {
        int original=ci.getReturnValue();
        if(original<=0 || !InitialWork.active())return;
        ci.setReturnValue(InitialWork.budget(original,getTotalThreadCount(),getBusyThreadCount(),getScheduledJobCount()));
    }
}
