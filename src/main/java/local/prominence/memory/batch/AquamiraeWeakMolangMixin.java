package local.prominence.memory.batch;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import dev.obscuria.aquamirae.common.entity.Scuttler;
import java.util.function.DoubleSupplier;
import local.prominence.memory.WeakDoubleSupplier;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import software.bernie.geckolib.core.molang.MolangParser;
@Mixin(targets="dev.obscuria.aquamirae.client.model.ModelScuttler",remap=false)
public abstract class AquamiraeWeakMolangMixin {
    @Shadow private static double molangWalkSpeed(Scuttler entity){throw new AssertionError();}
    @WrapOperation(method="applyMolangQueries(Ldev/obscuria/aquamirae/common/entity/Scuttler;D)V",at=@At(value="INVOKE",target="Lsoftware/bernie/geckolib/core/molang/MolangParser;setMemoizedValue(Ljava/lang/String;Ljava/util/function/DoubleSupplier;)V"),remap=false)
    private void prominence$weakWalkOwner(MolangParser parser,String name,DoubleSupplier ignored,Operation<Void> original,Scuttler entity,double animationTime) {
        original.call(parser,name,new WeakDoubleSupplier<Scuttler>(entity,AquamiraeWeakMolangMixin::molangWalkSpeed));
    }
}
