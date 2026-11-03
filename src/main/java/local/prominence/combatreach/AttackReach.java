package local.prominence.combatreach;

import com.jamieswhiteshirt.reachentityattributes.ReachEntityAttributes;
import net.minecraft.class_1657;

/** Reach Entity Attributes stores a zero-based bonus, not vanilla's base range. */
public final class AttackReach {
    private AttackReach() {}

    public static double apply(class_1657 player, double weaponRange) {
        if (player == null) return weaponRange;
        double result = ReachEntityAttributes.getAttackRange(player, weaponRange);
        return Double.isFinite(result) ? Math.max(0.0, result) : weaponRange;
    }
}
