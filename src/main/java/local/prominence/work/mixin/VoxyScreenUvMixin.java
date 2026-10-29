package local.prominence.work.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import local.prominence.work.VoxyScreenUvFix;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(targets="me.cortex.voxy.client.iris.IrisShaderPatch",remap=false)
public abstract class VoxyScreenUvMixin {
    @WrapMethod(method="fixEuphoriaVolumetricDepthUv",remap=false)
    private static String prominence$unboundDepth(String source,String[] uniforms,Operation<String> original) {
        return VoxyScreenUvFix.fix(original.call(source,uniforms),uniforms);
    }
}
