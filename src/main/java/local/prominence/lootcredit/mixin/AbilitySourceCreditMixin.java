package local.prominence.lootcredit.mixin;
import local.prominence.lootcredit.*;import net.minecraft.*;import org.spongepowered.asm.mixin.Mixin;import org.spongepowered.asm.mixin.injection.*;import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(targets="net.sweenus.simplyswords.util.HelperMethods",remap=false) public abstract class AbilitySourceCreditMixin {
 @Inject(method="applyAbilityDamageEnchantments",at=@At("HEAD")) private static void credit$origin(class_3218 world,class_1799 stack,class_1297 target,class_1282 source,float amount,CallbackInfoReturnable<Float> ci){if(source instanceof CreditHolder h)h.prominence$credit(Credit.capture(source.method_5529(),stack));}
}
