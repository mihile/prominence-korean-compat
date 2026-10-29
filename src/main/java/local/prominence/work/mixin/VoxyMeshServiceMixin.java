package local.prominence.work.mixin;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import local.prominence.work.VoxyMeshAdmission;
import me.cortex.voxy.common.thread.Service;
import me.cortex.voxy.common.thread.ServiceManager;
import me.cortex.voxy.common.util.Pair;
import java.util.function.Supplier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
@Mixin(targets="me.cortex.voxy.client.core.rendering.building.RenderGenerationService",remap=false)
public abstract class VoxyMeshServiceMixin {
 @WrapOperation(method="<init>",at=@At(value="INVOKE",target="Lme/cortex/voxy/common/thread/ServiceManager;createService(Ljava/util/function/Supplier;JLjava/lang/String;)Lme/cortex/voxy/common/thread/Service;"),remap=false)
 private Service prominence$limitMesh(ServiceManager manager,Supplier<Pair<Runnable,Runnable>> context,long weight,String name,Operation<Service> original){
  if(!"Section mesh generation service".equals(name))return original.call(manager,context,weight,name);
  return manager.createService(context,weight,name,VoxyMeshAdmission::allowed);
 }
}
