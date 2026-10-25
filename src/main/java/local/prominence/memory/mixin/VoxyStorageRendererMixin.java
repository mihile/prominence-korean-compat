package local.prominence.memory.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import local.prominence.memory.VoxyStorageRecovery;
import net.minecraft.class_761;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value=class_761.class,priority=300)
public abstract class VoxyStorageRendererMixin {
    @WrapMethod(method="voxy$createRenderer",remap=false)
    private void prominence$deferBusyStorage(Operation<Void> original){
        if(!VoxyStorageRecovery.rendererBlocked())original.call();
    }
}
