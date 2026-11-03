package local.prominence.cinder.mixin;
import local.prominence.cinder.CinderRewards;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(targets="elocindev.prominentbase.mythicchallenge.MythicChallenge",remap=false)
public class ChallengeRewardMixin {
    @Unique private int prominence$cinderFactor;
    @Unique private final java.util.Set<net.minecraft.class_3222> prominence$cinderSent=java.util.Collections.newSetFromMap(new java.util.WeakHashMap<>());
    @Inject(method="getTotalCinderReward",at=@At("RETURN"),cancellable=true)
    private void scale(CallbackInfoReturnable<Integer> cir){
        if(prominence$cinderFactor==0)prominence$cinderFactor=CinderRewards.multiplier();
        cir.setReturnValue(CinderRewards.scaled(cir.getReturnValue(),prominence$cinderFactor));
    }
    @Inject(method="sendEndPopups",at=@At("HEAD"))
    private void sync(CallbackInfo ci){
        if(prominence$cinderFactor==0)prominence$cinderFactor=CinderRewards.multiplier();
        CinderRewards.sync(prominence$cinderFactor,prominence$cinderSent);
    }
}
