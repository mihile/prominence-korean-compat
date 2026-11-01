package local.prominence.cinder.client.mixin;
import local.prominence.cinder.client.CinderClient;
import net.minecraft.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(targets="elocindev.prominentbase.client.gui.ChallengeEndScreen",remap=false)
public class ChallengeRewardLabelMixin {
    @Redirect(method="buildWinDisplay",at=@At(value="INVOKE",target="Lnet/minecraft/class_2561;method_43469(Ljava/lang/String;[Ljava/lang/Object;)Lnet/minecraft/class_5250;"))
    private class_5250 label(String key,Object[] args){
        if(key.equals("prominent.challenge_end.cinders")&&args.length==1&&args[0] instanceof Integer total&&CinderClient.multiplier>1&&total%CinderClient.multiplier==0){
            return class_2561.method_43469(key,total/CinderClient.multiplier)
                .method_10852(class_2561.method_43470(" ×"+CinderClient.multiplier).method_27692(class_124.field_1054));
        }
        return class_2561.method_43469(key,args);
    }
}
