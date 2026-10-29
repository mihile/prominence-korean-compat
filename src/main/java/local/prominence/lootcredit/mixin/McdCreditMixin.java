package local.prominence.lootcredit.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import local.prominence.lootcredit.*;
import net.minecraft.*;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(targets="chronosacaria.mcdw.effects.EnchantmentEffects",remap=false)
public abstract class McdCreditMixin {
 @WrapMethod(method={"causeShockwaveAttack","causeSmitingAttack","causeSwirlingAttack"})
 private static void credit$area(class_1309 user,class_1309 target,float distance,float amount,Operation<Void> original){try(var scope=CreditScope.enter(LootCredit.current(user),true)){original.call(user,target,distance,amount);}}
 @WrapMethod(method="electrocuteNearbyEnemies") private static void credit$thunder(class_1309 user,float distance,float amount,int limit,Operation<Void> original){try(var scope=CreditScope.enter(LootCredit.current(user),true)){original.call(user,distance,amount,limit);}}
 @WrapMethod(method={"applyShockwave","applySwirling"}) private static void credit$selected(class_1309 user,class_1309 target,float amount,boolean off,Operation<Void> original){try(var scope=CreditScope.enter(Credit.capture(user,off?user.method_6079():user.method_6047()),true)){original.call(user,target,amount,off);}}
 @WrapMethod(method="applyThundering") private static void credit$selectedThunder(class_1309 user,float amount,boolean off,Operation<Void> original){try(var scope=CreditScope.enter(Credit.capture(user,off?user.method_6079():user.method_6047()),true)){original.call(user,amount,off);}}
 @WrapMethod(method="applyThunderingShot") private static void credit$arrow(class_1309 user,class_1665 arrow,Operation<Void> original){var c=LootCredit.projectile(arrow);try(var scope=CreditScope.enter(c==null?LootCredit.current(user):c,true)){original.call(user,arrow);}}
}
