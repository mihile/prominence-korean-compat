package local.prominence.work.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import local.prominence.work.AttributeCandidates;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(targets="dev.emi.emi.search.EmiSearch",remap=false)
public abstract class EmiAttributeScopeMixin {
    @WrapMethod(method="bake",remap=false)
    private static void prominence$attributeCandidates(Operation<Void> original){
        try(var scope=AttributeCandidates.begin()){original.call();}
    }
}
