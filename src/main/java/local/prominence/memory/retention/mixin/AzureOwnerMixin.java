package local.prominence.memory.retention.mixin;

import local.prominence.memory.*;
import net.minecraft.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets="mod.azure.azurelib.rewrite.render.AzRendererPipelineContext",remap=false)
public abstract class AzureOwnerMixin implements RetainedClientOwner {
    @Shadow protected Object animatable;
    @Shadow private class_4597 multiBufferSource;
    @Shadow private class_4587 poseStack;
    @Shadow private class_4588 vertexConsumer;
    @Inject(method="<init>",at=@At("RETURN"))
    private void prominence$track(CallbackInfo ci){RetentionCleanupClient.trackAzure(this);}
    public Object prominence$retainedOwner(){return animatable;}
    public void prominence$releaseOwner(){animatable=null;multiBufferSource=null;poseStack=null;vertexConsumer=null;}
}
