package local.prominence.work.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import local.prominence.work.DeferredOpacityEntry;
import local.prominence.work.MapperLoadScope;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.objectweb.asm.Opcodes;

@Mixin(targets="me.cortex.voxy.common.world.other.Mapper", remap=false)
public abstract class VoxyDeferredOpacityMapperMixin {
    @WrapMethod(method="loadFromStorage", remap=false)
    private void prominence$loadingMappings(Operation<Void> original) {
        try (var scope=MapperLoadScope.begin()) { original.call(); }
    }
    @WrapOperation(method="getBlockStateOpacity(I)I", at=@At(value="FIELD", target="Lme/cortex/voxy/common/world/other/Mapper$StateEntry;opacity:I", opcode=Opcodes.GETFIELD), remap=false)
    private int prominence$resolveOpacity(@Coerce Object entry, Operation<Integer> original) {
        return ((DeferredOpacityEntry) entry).prominence$resolvedOpacity();
    }
}
