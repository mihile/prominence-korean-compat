package local.prominence.work.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import local.prominence.work.InitialWork;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(targets="dev.emi.emi.runtime.EmiReloadManager$ReloadWorker",remap=false)
public abstract class EmiInitialWorkMixin {
    @WrapMethod(method="run",remap=false)
    private void prominence$coordinateInitialTerrain(Operation<Void> original) {
        try(var scope=InitialWork.begin()){original.call();}
    }
}
