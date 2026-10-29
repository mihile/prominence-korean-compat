package local.prominence.lootcredit.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import local.prominence.lootcredit.*;
import net.minecraft.*;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(targets={"net.sweenus.simplyswords.power.powers.ImbuedPower","net.sweenus.simplyswords.power.powers.PincushionPower","net.sweenus.simplyswords.power.powers.EchoPower"},remap=false)
public abstract class GemCreditMixin {
 @WrapMethod(method="postHit") private void credit$hit(class_1799 stack,class_1309 target,class_1309 actor,Operation<Void> original){try(var scope=CreditScope.enter(Credit.capture(actor,stack),true)){original.call(stack,target,actor);}}
}
