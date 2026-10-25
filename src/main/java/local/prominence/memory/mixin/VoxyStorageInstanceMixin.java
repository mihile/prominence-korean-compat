package local.prominence.memory.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import local.prominence.memory.VoxyStorageRecovery;
import me.cortex.voxy.common.world.WorldEngine;
import me.cortex.voxy.commonImpl.VoxyInstance;
import me.cortex.voxy.commonImpl.WorldIdentifier;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value=VoxyInstance.class,remap=false)
public abstract class VoxyStorageInstanceMixin {
    @WrapMethod(method="getOrCreate(Lme/cortex/voxy/commonImpl/WorldIdentifier;Z)Lme/cortex/voxy/common/world/WorldEngine;",remap=false)
    private WorldEngine prominence$recoverStorage(WorldIdentifier id,boolean acquire,Operation<WorldEngine> original){
        return VoxyStorageRecovery.open((VoxyInstance)(Object)this,id,acquire,original);
    }
}
