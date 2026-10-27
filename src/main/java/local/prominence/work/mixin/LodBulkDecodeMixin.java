package local.prominence.work.mixin;
import com.dripps.voxyserver.network.LODBulkPayload;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import local.prominence.work.LodBulkDecode;
import local.prominence.work.RemoteIngestScratch;
import net.minecraft.class_2540;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(targets="com.dripps.voxyserver.network.LODBulkPayload",remap=false)
public abstract class LodBulkDecodeMixin {
    @WrapMethod(method="read",remap=false)
    private static LODBulkPayload prominence$decode(class_2540 buf,Operation<LODBulkPayload> original){
        return RemoteIngestScratch.active()?LodBulkDecode.read(buf):original.call(buf);
    }
}
