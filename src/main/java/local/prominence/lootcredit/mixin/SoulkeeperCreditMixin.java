package local.prominence.lootcredit.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import local.prominence.lootcredit.*;
import net.minecraft.*;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(targets="net.sweenus.simplyswords.world.SoulkeeperLanternManager",remap=false)
public abstract class SoulkeeperCreditMixin {
 @WrapMethod(method="damageTarget") private static boolean credit$hit(class_3218 world,class_1309 actor,class_1309 target,float amount,Operation<Boolean> original){try(var scope=CreditScope.enter(LootCredit.current(actor),true)){return original.call(world,actor,target,amount);}}
}
