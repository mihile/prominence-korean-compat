package local.prominence.memory.mixin;

import local.prominence.memory.ShaderParseCacheRelease;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.irisshaders.iris.pipeline.IrisRenderingPipeline", remap = false)
public abstract class IrisPipelineCacheLifetimeMixin {
    @Inject(method = "<init>", at = @At("RETURN"), remap = false, require = 1)
    private void prominence$releaseCompletedParser(CallbackInfo ci) {
        ShaderParseCacheRelease.afterPipelineBuilt();
    }
}
