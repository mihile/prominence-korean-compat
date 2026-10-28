package local.prominence.work.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import local.prominence.work.DeferredOpacity;
import local.prominence.work.DeferredOpacityEntry;
import local.prominence.work.MapperLoadScope;
import net.minecraft.class_1922;
import net.minecraft.class_2338;
import net.minecraft.class_2680;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets="me.cortex.voxy.common.world.other.Mapper$StateEntry", remap=false)
public abstract class VoxyDeferredOpacityEntryMixin implements DeferredOpacityEntry {
    @Shadow @Final @Mutable public int opacity;
    @Unique private volatile DeferredOpacity prominence$pendingOpacity;

    @WrapOperation(method="<init>", at=@At(value="INVOKE", target="Lnet/minecraft/class_2680;method_26193(Lnet/minecraft/class_1922;Lnet/minecraft/class_2338;)I"), remap=false)
    private int prominence$deferStoredOpacity(class_2680 state, class_1922 view, class_2338 pos, Operation<Integer> original) {
        if (!MapperLoadScope.defer()) return original.call(state, view, pos);
        prominence$pendingOpacity = new DeferredOpacity(() -> original.call(state, view, pos));
        return 0; // All native reads are routed through the pinned Mapper getter below.
    }
    public int prominence$resolvedOpacity() {
        DeferredOpacity pending = prominence$pendingOpacity;
        if (pending == null) return opacity; // Includes native leaves=15 and non-loaded entries.
        int resolved = pending.get();
        opacity = resolved;
        prominence$pendingOpacity = null; // Publish native field and release the per-entry helper.
        return resolved;
    }
}
