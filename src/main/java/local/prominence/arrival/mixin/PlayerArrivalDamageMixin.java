package local.prominence.arrival.mixin;
import local.prominence.arrival.ArrivalServer;
import net.minecraft.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(class_3222.class)
public abstract class PlayerArrivalDamageMixin {
    @Inject(method="method_5643",at=@At("HEAD"),cancellable=true,remap=false)
    private void prominence$arrivalDamage(class_1282 source,float amount,CallbackInfoReturnable<Boolean> result){
        if(!source.method_49708(class_8111.field_42347)&&!source.method_49708(class_8111.field_44869)&&ArrivalServer.protectedNow((class_3222)(Object)this))result.setReturnValue(false);
    }
}
