package local.prominence.combatreach.mixin;

import local.prominence.combatreach.AttackReach;
import net.bettercombat.api.WeaponAttributes;
import net.minecraft.class_1297;
import net.minecraft.class_1657;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(targets = "net.bettercombat.client.collision.TargetFinder", remap = false)
public abstract class TargetFinderReachMixin {
    @ModifyVariable(method = "findAttackTargetResult(Lnet/minecraft/class_1657;Lnet/minecraft/class_1297;Lnet/bettercombat/api/WeaponAttributes$Attack;D)Lnet/bettercombat/client/collision/TargetFinder$TargetResult;",
            at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 1)
    private static double prominence$includeAttackReach(double range, class_1657 player, class_1297 cursor,
                                                       WeaponAttributes.Attack attack, double originalRange) {
        return AttackReach.apply(player, range);
    }
}
