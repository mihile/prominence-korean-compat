package local.prominence.lootcredit.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import local.prominence.lootcredit.*;
import net.minecraft.*;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(targets="net.soulsweaponry.entity.projectile.LeviathanAxeEntity",remap=false)
public abstract class LeviathanCreditMixin {
 @WrapMethod(method="collide") private boolean credit$hit(class_1297 owner,class_1297 target,float damage,Operation<Boolean> original){var c=LootCredit.projectile((class_1297)(Object)this);try(var scope=CreditScope.enter(c==null?LootCredit.current(owner):c,true)){return original.call(owner,target,damage);}}
}
