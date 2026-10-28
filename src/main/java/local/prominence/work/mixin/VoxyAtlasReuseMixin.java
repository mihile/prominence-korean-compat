package local.prominence.work.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import local.prominence.work.VoxyAtlasReuseState;
import me.cortex.voxy.client.core.RenderResourceReuse;
import me.cortex.voxy.client.core.model.bakery.ModelAtlasReadback;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets="me.cortex.voxy.client.core.model.bakery.SoftwareModelTextureBakery", remap=false)
public abstract class VoxyAtlasReuseMixin {

    @WrapOperation(method="_doSetupTexture", at=@At(value="INVOKE",
        target="Lme/cortex/voxy/client/core/RenderResourceReuse;acquireModelSourceBuffer(I)Lme/cortex/voxy/client/core/RenderResourceReuse$ModelSourceBuffer;"), remap=false)
    private RenderResourceReuse.ModelSourceBuffer prominence$onAcquireBuffer(int pixelCount, Operation<RenderResourceReuse.ModelSourceBuffer> original) {
        RenderResourceReuse.ModelSourceBuffer buffer = original.call(pixelCount);
        VoxyAtlasReuseState.onAcquire(buffer != null && buffer.reused(), pixelCount);
        return buffer;
    }

    @WrapOperation(method="_doSetupTexture", at=@At(value="INVOKE",
        target="Lme/cortex/voxy/client/core/model/bakery/ModelAtlasReadback;tryBegin(I[I)Lme/cortex/voxy/client/core/model/bakery/ModelAtlasReadback;"), remap=false)
    private ModelAtlasReadback prominence$bypassPboDump(int glId, int[] pixels, Operation<ModelAtlasReadback> original) {
        if (VoxyAtlasReuseState.shouldSkipReadback(glId, pixels != null ? pixels.length : 0)) {
            return null;
        }
        VoxyAtlasReuseState.onTextureInstalled(glId);
        return original.call(glId, pixels);
    }

    @WrapOperation(method="_doSetupTexture", at=@At(value="INVOKE",
        target="Lme/cortex/voxy/client/core/model/bakery/ModelAtlasReadback;readSynchronously(I[I)V"), remap=false)
    private void prominence$bypassSyncReadback(int glId, int[] pixels, Operation<Void> original) {
        if (VoxyAtlasReuseState.shouldSkipReadback(glId, pixels != null ? pixels.length : 0)) {
            VoxyAtlasReuseState.logReuse(glId, pixels != null ? pixels.length : 0);
            return;
        }
        VoxyAtlasReuseState.onTextureInstalled(glId);
        original.call(glId, pixels);
    }
}
