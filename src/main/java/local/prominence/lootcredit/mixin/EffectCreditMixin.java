package local.prominence.lootcredit.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import local.prominence.lootcredit.*;
import net.minecraft.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(class_1293.class)
public abstract class EffectCreditMixin implements CreditHolder {
 @Unique private Credit credit$value;
 public Credit prominence$credit(){return credit$value;}public void prominence$credit(Credit c){credit$value=c;}
 @Inject(method="<init>(Lnet/minecraft/class_1293;)V",at=@At("RETURN")) private void credit$copy(class_1293 source,CallbackInfo ci){credit$value=((CreditHolder)source).prominence$credit();}
 @Inject(method="method_24276",at=@At("TAIL")) private void credit$promote(class_1293 source,CallbackInfo ci){credit$value=((CreditHolder)source).prominence$credit();}
 @WrapMethod(method="method_5589") private void credit$tick(class_1309 entity,Operation<Void> original){try(var scope=CreditScope.enter(credit$value,true)){original.call(entity);}}
 @Inject(method="method_5582",at=@At("RETURN")) private void credit$save(class_2487 tag,CallbackInfoReturnable<class_2487> ci){if(credit$value!=null)credit$value.write(ci.getReturnValue());}
 @Inject(method="method_24275",at=@At("RETURN")) private static void credit$load(class_1291 type,class_2487 tag,CallbackInfoReturnable<class_1293> ci){if(ci.getReturnValue() instanceof CreditHolder h)h.prominence$credit(Credit.read(tag));}
}
