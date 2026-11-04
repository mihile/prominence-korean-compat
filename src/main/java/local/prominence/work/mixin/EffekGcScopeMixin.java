package local.prominence.work.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;import com.llamalad7.mixinextras.injector.wrapoperation.Operation;import local.prominence.work.EffekGcScope;import org.spongepowered.asm.mixin.Mixin;
@Mixin(targets="mod.chloeprime.aaaparticles.api.client.EffectRegistry",remap=false) public abstract class EffekGcScopeMixin {
 @WrapMethod(method="gc") private static void prominence$scopeGc(Operation<Void> original){EffekGcScope.enter();try{original.call();}finally{EffekGcScope.exit();}}
}
