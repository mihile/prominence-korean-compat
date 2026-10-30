package local.prominence.arrival.mixin;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import local.prominence.arrival.ArrivalServer;
import net.minecraft.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets="wraith.fwaystones.block.WaystoneBlockEntity",remap=false)
public abstract class WaystoneArrivalMixin {
    @WrapOperation(method="doTeleport",at=@At(value="INVOKE",target="Lnet/fabricmc/fabric/api/dimension/v1/FabricDimensions;teleport(Lnet/minecraft/class_1297;Lnet/minecraft/class_3218;Lnet/minecraft/class_5454;)Lnet/minecraft/class_1297;"),remap=false)
    private static class_1297 prominence$arrival(class_1297 entity,class_3218 destination,class_5454 target,Operation<class_1297> original){
        if(!(entity instanceof class_3222 player))return original.call(entity,destination,target);
        var attempt=ArrivalServer.begin(player);
        try{class_1297 result=original.call(entity,destination,target);if(result==player)ArrivalServer.arrived(attempt,destination);else ArrivalServer.abort(attempt);return result;}
        catch(RuntimeException|Error failure){ArrivalServer.abort(attempt);throw failure;}
    }
}
