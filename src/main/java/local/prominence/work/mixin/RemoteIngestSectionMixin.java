package local.prominence.work.mixin;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import local.prominence.work.RemoteIngestScratch;
import me.cortex.voxy.common.voxelization.VoxelizedSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
@Mixin(targets="com.dripps.voxyserver.client.service.RemoteIngestService",remap=false)
public abstract class RemoteIngestSectionMixin {
    @WrapOperation(method="process",at=@At(value="INVOKE",target="Lme/cortex/voxy/common/voxelization/VoxelizedSection;createEmpty()Lme/cortex/voxy/common/voxelization/VoxelizedSection;"),remap=false)
    private VoxelizedSection prominence$reuseSection(Operation<VoxelizedSection> original){return RemoteIngestScratch.acquire(()->original.call());}
}
