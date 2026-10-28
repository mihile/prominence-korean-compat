package local.prominence.work.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import local.prominence.work.EarlyRendererGate;
import local.prominence.work.InitialWork;
import me.cortex.voxy.client.core.IGetVoxyRenderSystem;
import me.cortex.voxy.client.core.util.IrisUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Iris initializes block IDs then reloads the renderer on its first frame.
 *  Let Voxy's existing Iris hook create the renderer after that reload. */
@Mixin(targets="com.dripps.voxyserver.client.ClientLodReceiver",remap=false)
public abstract class VoxyServerEarlyRendererMixin {
    @WrapOperation(method="tick",at=@At(value="INVOKE",target="Lme/cortex/voxy/client/core/IGetVoxyRenderSystem;voxy$createRenderer()V"),remap=false)
    private static void prominence$deferShaderRenderer(IGetVoxyRenderSystem renderer,Operation<Void> original) {
        InitialWork.ensureRenderThreadPriority();
        EarlyRendererGate.invoke(IrisUtil.irisShaderPackEnabled(), () -> original.call(renderer));
    }
}
