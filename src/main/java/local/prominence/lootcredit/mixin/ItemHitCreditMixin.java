package local.prominence.lootcredit.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import local.prominence.lootcredit.*;
import net.minecraft.*;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(class_1799.class)
public abstract class ItemHitCreditMixin {
 @WrapMethod(method="method_7979") private void credit$hit(class_1309 target,class_1657 player,Operation<Void> original){try(var scope=CreditScope.enter(Credit.capture(player,(class_1799)(Object)this),false)){original.call(target,player);}}
}
