package local.prominence.work.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import local.prominence.work.InitialWork;
import local.prominence.work.ModelWorkSlice;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;

@Mixin(targets="me.cortex.voxy.client.core.model.ModelFactory", remap=false)
public abstract class VoxyModelWorkSliceMixin {
    @WrapMethod(method="processAllThings", remap=false)
    private void prominence$sliceDuringEmi(Operation<Void> original) {
        if (!InitialWork.active()) { original.call(); return; }
        try (var slice=ModelWorkSlice.begin()) { original.call(); }
    }
    @WrapOperation(method="processAllThings", at=@At(value="INVOKE",
        target="Lme/cortex/voxy/client/core/model/ModelFactory;processModelResult()Z"), remap=false)
    private boolean prominence$yieldBetweenModels(@Coerce Object factory, Operation<Boolean> original) {
        return ModelWorkSlice.continueBatch(original.call(factory));
    }
}
