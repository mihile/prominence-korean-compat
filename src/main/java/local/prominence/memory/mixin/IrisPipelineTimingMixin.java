package local.prominence.memory.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import local.prominence.memory.ShaderPreprocessBridge;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.irisshaders.iris.shaderpack.materialmap.NamespacedId;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(targets="net.irisshaders.iris.pipeline.PipelineManager",remap=false)
public abstract class IrisPipelineTimingMixin {
    @WrapMethod(method="preparePipeline",remap=false)
    private WorldRenderingPipeline prominence$measurePreparation(NamespacedId dimension,Operation<WorldRenderingPipeline> original) {
        long start=System.nanoTime();
        try{return original.call(dimension);}finally{ShaderPreprocessBridge.report(System.nanoTime()-start,dimension);}
    }

}
