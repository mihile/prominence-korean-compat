package local.prominence.memory.batch;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import java.util.List;
import java.util.function.Predicate;
import local.prominence.memory.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets="eu.ha3.presencefootsteps.sound.player.DelayedSoundPlayer",remap=false)
public abstract class FootstepQueueMixin {
    @Shadow private long nextPlayTime;
    @WrapOperation(method="playSound",at=@At(value="INVOKE",target="Ljava/util/List;add(Ljava/lang/Object;)Z"),remap=false)
    private boolean prominence$wakeNewSound(List<Object> queue,Object sound,Operation<Boolean> original) {
        boolean added=original.call(queue,sound);
        if(added)nextPlayTime=FootstepTiming.wake(nextPlayTime,((FootstepPendingAccess)sound).prominence$dueTime());
        return added;
    }
    @WrapOperation(method="think",at=@At(value="INVOKE",target="Ljava/util/List;removeIf(Ljava/util/function/Predicate;)Z"),remap=false)
    private boolean prominence$nextUnplayedSound(List<Object> queue,Predicate<Object> predicate,Operation<Boolean> original) {
        boolean changed=original.call(queue,predicate);nextPlayTime=FootstepTiming.earliest(queue);return changed;
    }
}
