package local.prominence.swingscope.mixin;
import local.prominence.swingscope.SwingScope;
import net.minecraft.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets="net.sweenus.simplyswords.api.SimplySwordsAPI",remap=false)
public abstract class WeaponSwingApiMixin {
    @Inject(method="onWeaponSwing(Lnet/minecraft/class_1799;Lnet/minecraft/class_3218;Lnet/minecraft/class_1309;Lnet/minecraft/class_1268;)V",at=@At("HEAD"),cancellable=true,require=1)
    private static void prominence$combatOnly(class_1799 stack,class_3218 world,class_1309 actor,class_1268 hand,CallbackInfo ci){
        if(SwingScope.blockAnimationPower(actor))ci.cancel();
    }
}
