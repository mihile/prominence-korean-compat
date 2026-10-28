package local.prominence.lootcredit.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;import com.llamalad7.mixinextras.injector.wrapoperation.Operation;import local.prominence.lootcredit.*;import net.minecraft.*;import org.spongepowered.asm.mixin.Mixin;
@Mixin(targets="net.sweenus.simplyswords.api.SimplySwordsAPI",remap=false) public abstract class SimplyApiCreditMixin {
 @WrapMethod(method="applyEntityWeaponHit") private static boolean credit$hit(class_1799 stack,class_1309 target,class_1309 actor,float amount,Operation<Boolean> original){try(var scope=CreditScope.enter(Credit.capture(actor,stack),false)){return original.call(stack,target,actor,amount);}}
 @WrapMethod(method="applyEntityWeaponPostHit") private static void credit$post(class_1799 stack,class_1309 target,class_1309 actor,float amount,Operation<Void> original){try(var scope=CreditScope.enter(Credit.capture(actor,stack),false)){original.call(stack,target,actor,amount);}}
}
