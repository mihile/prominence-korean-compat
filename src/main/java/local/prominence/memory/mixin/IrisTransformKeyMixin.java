package local.prominence.memory.mixin;

import local.prominence.memory.ShaderTransformKey;
import net.irisshaders.iris.pipeline.transform.parameter.Parameters;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.irisshaders.iris.pipeline.transform.TransformPatcher$CacheKey", remap = false)
public abstract class IrisTransformKeyMixin {
    @Shadow @Final @Mutable private Parameters parameters;

    @Inject(method = {
        "<init>(Lnet/irisshaders/iris/pipeline/transform/parameter/Parameters;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V",
        "<init>(Lnet/irisshaders/iris/pipeline/transform/parameter/Parameters;Ljava/lang/String;)V"
    }, at = @At("RETURN"), remap = false, require = 2)
    private void prominence$freezeInputs(CallbackInfo ci) {
        parameters = ShaderTransformKey.snapshot(parameters);
    }
}
