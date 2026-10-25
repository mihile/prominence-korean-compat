package local.prominence.memory.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import local.prominence.memory.ShaderPreprocessBridge;
import net.irisshaders.iris.helpers.StringPair;
import net.irisshaders.iris.shaderpack.preprocessor.GlslCollectingListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets="net.irisshaders.iris.shaderpack.preprocessor.JcppProcessor",remap=false)
public abstract class IrisPreprocessDiskCacheMixin {
    @WrapMethod(method="glslPreprocessSource",remap=false)
    private static String prominence$reuseText(String source,Iterable<StringPair> defines,Operation<String> original) {
        return ShaderPreprocessBridge.preprocess(source,defines,original);
    }
    @ModifyExpressionValue(method="glslPreprocessSource",at=@At(value="NEW",target="net/irisshaders/iris/shaderpack/preprocessor/GlslCollectingListener"),remap=false)
    private static GlslCollectingListener prominence$observeDiagnostics(GlslCollectingListener listener) {
        ShaderPreprocessBridge.observe(listener);return listener;
    }
}
