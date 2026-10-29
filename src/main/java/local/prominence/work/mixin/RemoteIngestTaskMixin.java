package local.prominence.work.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import local.prominence.work.RemoteIngestScratch;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(targets="com.dripps.voxyserver.client.service.RemoteIngestService$IngestTask",remap=false)
public abstract class RemoteIngestTaskMixin {
    @WrapMethod(method="run",remap=false)
    private void prominence$ingestScope(Operation<Void> original){
        try(var scope=RemoteIngestScratch.begin()){original.call();}
    }
}
