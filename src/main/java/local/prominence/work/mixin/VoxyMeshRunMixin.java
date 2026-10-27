package local.prominence.work.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import local.prominence.work.VoxyMeshAdmission;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
@Mixin(targets="me.cortex.voxy.common.thread.Service",remap=false)
public abstract class VoxyMeshRunMixin {
 @Shadow @Final private String name;
 @WrapMethod(method="runJob",remap=false)
 private boolean prominence$meshAdmission(Operation<Boolean> original){
  if(!"Section mesh generation service".equals(name))return original.call();
  if(!VoxyMeshAdmission.enter())return false;
  try{return original.call();}finally{VoxyMeshAdmission.leave();}
 }
}
