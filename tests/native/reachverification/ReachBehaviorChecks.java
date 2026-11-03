package reachverification;

import java.util.List;
import local.prominence.combatreach.AttackReach;
import net.bettercombat.api.WeaponAttributes;
import net.bettercombat.api.client.AttackRangeExtensions;
import net.bettercombat.client.collision.TargetFinder;
import net.minecraft.class_1297;
import net.minecraft.class_1657;

public final class ReachBehaviorChecks {
    private static int checks;
    private static void check(boolean value) { if (!value) throw new AssertionError("check " + checks); checks++; }
    private static void close(double actual, double expected) { check(Math.abs(actual - expected) < 1e-9); }
    public static void main(String[] args) {
        boolean fixed = args[0].equals("fixed");
        var player = new class_1657();
        var attack = new WeaponAttributes.Attack(90.0);
        for (double base : new double[]{2.0, 3.0, 4.5, 7.0}) for (double bonus : new double[]{0, 2, 0.5, -1, -10, 20}) {
            player.attackBonus = bonus;
            player.targets = List.of(new class_1297(base + 1), new class_1297(base + 2.5));
            AttackRangeExtensions.sources().clear();
            Trace.reset();
            var result = TargetFinder.findAttackTargetResult(player, null, attack, base);
            double expected = fixed ? Math.max(0, base + bonus) : base;
            close(Trace.queryRange, expected); close(Trace.hitboxRange, expected); close(Trace.radialRange, expected);
            check(result.entities.size() == player.targets.stream().filter(e -> e.distance <= expected).count());
            check(Trace.attributeReads == (fixed ? 1 : 0));
        }
        // Distances unreachable with a 3-block weapon become reachable with the extender.
        player.attackBonus = 2.0; player.targets = List.of(new class_1297(4.0), new class_1297(5.5));
        var result = TargetFinder.findAttackTargetResult(player, null, attack, 3.0);
        check(result.entities.size() == (fixed ? 1 : 0));
        // Large bonuses must expand broad-phase candidates too, not only the final hitbox.
        player.attackBonus = 20.0; player.targets = List.of(new class_1297(19.0));
        check(TargetFinder.findAttackTargetResult(player, null, attack, 3.0).entities.size() == (fixed ? 1 : 0));
        // Existing extension ordering (ADD then MULTIPLY, including Pehkui's native API) is unchanged.
        for (double scale : new double[]{0.5, 1, 2}) {
            AttackRangeExtensions.sources().clear();
            AttackRangeExtensions.register(context -> new AttackRangeExtensions.Modifier(scale, AttackRangeExtensions.Operation.MULTIPLY));
            AttackRangeExtensions.register(context -> new AttackRangeExtensions.Modifier(1.0, AttackRangeExtensions.Operation.ADD));
            player.attackBonus = 2.0; player.targets = List.of(); Trace.reset();
            TargetFinder.findAttackTargetResult(player, null, attack, 3.0);
            close(Trace.queryRange, fixed ? 5 : 3);
            close(Trace.hitboxRange, ((fixed ? 5 : 3) + 1) * scale);
            close(Trace.radialRange, Trace.hitboxRange);
            check(Trace.attributeReads == (fixed ? 1 : 0));
        }
        AttackRangeExtensions.sources().clear();
        // Re-equipping and block-only buffs never leave cached or invented attack bonuses.
        for (Double bonus : new Double[]{2.0, 0.0, 2.0, null, Double.NaN, Double.POSITIVE_INFINITY}) {
            player.attackBonus = bonus; player.blockBonus = 8; Trace.reset();
            TargetFinder.findAttackTargetResult(player, null, attack, 3.0);
            close(Trace.hitboxRange, fixed && bonus != null && Double.isFinite(bonus) ? 3 + bonus : 3);
        }
        close(AttackReach.apply(null, 3.0), 3.0);
        System.out.println("PASS: " + checks + " " + args[0] + " native targeting/API checks; world/geometry doubles, no game launched");
    }
}
