package local.prominence.memory.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import local.prominence.memory.VoxyStorageRecovery;
import me.cortex.voxy.common.world.WorldEngine;
import me.cortex.voxy.commonImpl.WorldIdentifier;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value=WorldIdentifier.class,remap=false)
public abstract class VoxyStorageIdentifierMixin {
    @WrapMethod(method="getOrCreateEngine",remap=false)
    private WorldEngine prominence$unavailableIsNullable(Operation<WorldEngine> original){
        try{return original.call();}catch(IllegalStateException failure){
            // Preserve all unrelated native invariant failures. Only the reviewed null assertion
            // following our own confirmed storage conflict is recoverable.
            if("Engine null on creation".equals(failure.getMessage())&&VoxyStorageRecovery.unavailable((WorldIdentifier)(Object)this))return null;
            throw failure;
        }
    }
}
