package local.prominence.memory.batch;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalLongRef;
import local.prominence.memory.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets="eu.ha3.presencefootsteps.sound.player.DelayedSoundPlayer$PendingSound",remap=false)
public abstract class FootstepPendingMixin implements FootstepPendingAccess {
    @Accessor("timeToPlay") public abstract long prominence$dueTime();
    @ModifyExpressionValue(method="<init>",at=@At(value="INVOKE",target="Ljava/lang/System;currentTimeMillis()J"),remap=false)
    private long prominence$captureScheduleTime(long now,@Share("prominence$now") LocalLongRef time){time.set(now);return now;}
    @WrapOperation(method="<init>",at=@At(value="INVOKE",target="Ljava/lang/Math;max(JJ)J"),remap=false)
    private long prominence$relativeSchedule(long delay,long next,Operation<Long> original,@Share("prominence$now") LocalLongRef time){
        return original.call(delay,FootstepTiming.relativeNext(time.get(),next));
    }
    @Inject(method="tick",at=@At(value="INVOKE",target="Leu/ha3/presencefootsteps/sound/player/SoundPlayer;playSound(Lnet/minecraft/class_1309;Ljava/lang/String;FFLeu/ha3/presencefootsteps/sound/Options;)V",shift=At.Shift.AFTER),cancellable=true,remap=false)
    private void prominence$consumePlayedSound(CallbackInfoReturnable<Boolean> result){result.setReturnValue(true);}
}
