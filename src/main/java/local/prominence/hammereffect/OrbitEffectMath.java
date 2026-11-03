package local.prominence.hammereffect;

import java.lang.reflect.Field;
import java.util.List;
import net.minecraft.class_1309;
import net.minecraft.class_243;
import net.minecraft.class_310;
import net.minecraft.class_3532;
import net.minecraft.class_4184;
import net.minecraft.class_4587;
import net.minecraft.class_4597;
import net.minecraft.class_746;
import net.minecraft.class_897;
import net.minecraft.class_898;
import net.spell_engine.api.effect.CustomModelStatusEffect;
import net.spell_engine.api.effect.Synchronized;
import net.sweenus.simplyskills.client.effects.OrbitingRotatingEffectRenderer;

public final class OrbitEffectMath {
    private static final Field IRIS_SHADOW_ACTIVE = resolveIrisShadowField();

    private OrbitEffectMath() {}

    private static Field resolveIrisShadowField() {
        try {
            Class<?> cls = Class.forName("net.irisshaders.iris.shadows.ShadowRenderer", false, OrbitEffectMath.class.getClassLoader());
            Field f = cls.getDeclaredField("ACTIVE");
            f.setAccessible(true);
            return f;
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static boolean isIrisShadowPassActive() {
        if (IRIS_SHADOW_ACTIVE == null) return false;
        try {
            return IRIS_SHADOW_ACTIVE.getBoolean(null);
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * Computes `(ticks + tickDelta) * speed + offsetDegrees` wrapped into `[0, 360)` in double precision
     * before narrowing to float, preventing float32 tick-delta loss and JOML large-angle quantization
     * when `world.getTime()` exceeds 2^24 ticks.
     */
    public static float wrappedAngle(long ticks, float tickDelta, float speed, float offsetDegrees) {
        double deg = (((double) ticks * (double) speed) % 360.0)
            + (double) tickDelta * (double) speed
            + (double) offsetDegrees;
        deg %= 360.0;
        if (deg < 0.0) {
            deg += 360.0;
        }
        return (float) deg;
    }

    public static boolean isOrbitingRenderer(CustomModelStatusEffect.Renderer renderer) {
        return renderer instanceof OrbitingRotatingEffectRenderer
            || renderer instanceof net.sweenus.simplyskills.client.effects.OrbitingEffectRenderer
            || renderer instanceof net.spell_engine.api.render.OrbitingEffectRenderer;
    }

    public static boolean shouldRenderFirstPersonPass(boolean isThirdPerson, boolean isFocusedPlayer,
                                                      boolean isSpectator, boolean isSleeping) {
        return !isThirdPerson && isFocusedPlayer && !isSpectator && !isSleeping;
    }

    public static void renderFirstPersonOrbitingEffects(class_310 client, class_4184 camera, class_898 dispatcher,
                                                        float tickDelta, class_4587 matrices,
                                                        class_4597 vertexConsumers) {
        if (client == null || camera == null || dispatcher == null || matrices == null || vertexConsumers == null) {
            return;
        }
        class_746 player = client.field_1724;
        if (player == null || !shouldRenderFirstPersonPass(
                camera.method_19333(), camera.method_19331() == player, player.method_7325(), player.method_6113())) {
            return;
        }
        List<Synchronized.Effect> effects = Synchronized.effectsOf(player);
        if (effects == null || effects.isEmpty()) {
            return;
        }
        boolean hasOrbiting = false;
        for (Synchronized.Effect entry : effects) {
            if (entry != null && entry.effect() != null && isOrbitingRenderer(CustomModelStatusEffect.rendererOf(entry.effect()))) {
                hasOrbiting = true;
                break;
            }
        }
        if (!hasOrbiting) {
            return;
        }

        class_243 camPos = camera.method_19326();
        double lerpX = class_3532.method_16436((double) tickDelta, player.field_6038, player.method_23317());
        double lerpY = class_3532.method_16436((double) tickDelta, player.field_5971, player.method_23318());
        double lerpZ = class_3532.method_16436((double) tickDelta, player.field_5989, player.method_23321());
        class_897<? super class_746> entityRenderer = dispatcher.method_3953(player);
        class_243 offset = entityRenderer != null ? entityRenderer.method_23169(player, tickDelta) : class_243.field_1353;
        double dx = (lerpX - camPos.field_1352) + offset.field_1352;
        double dy = (lerpY - camPos.field_1351) + offset.field_1351;
        double dz = (lerpZ - camPos.field_1350) + offset.field_1350;
        int light = dispatcher.method_23839(player, tickDelta);

        matrices.method_22903();
        try {
            matrices.method_22904(dx, dy, dz);
            for (Synchronized.Effect entry : effects) {
                if (entry == null || entry.effect() == null) continue;
                CustomModelStatusEffect.Renderer renderer = CustomModelStatusEffect.rendererOf(entry.effect());
                if (isOrbitingRenderer(renderer)) {
                    renderer.renderEffect(entry.amplifier(), player, tickDelta, matrices, vertexConsumers, light);
                }
            }
        } finally {
            matrices.method_22909();
        }
    }

    public static void renderShadowPassFirstPersonEffects(class_1309 livingEntity, float tickDelta,
                                                          class_4587 matrices, class_4597 vertexConsumers, int light) {
        if (!isIrisShadowPassActive() || livingEntity == null || matrices == null || vertexConsumers == null) {
            return;
        }
        class_310 client = class_310.method_1551();
        if (client == null || livingEntity != client.field_1724 || client.field_1773 == null) {
            return;
        }
        if (client.field_1773.method_19418().method_19333()) {
            return;
        }
        List<Synchronized.Effect> effects = Synchronized.effectsOf(livingEntity);
        if (effects == null || effects.isEmpty()) {
            return;
        }
        for (Synchronized.Effect entry : effects) {
            if (entry == null || entry.effect() == null) continue;
            CustomModelStatusEffect.Renderer renderer = CustomModelStatusEffect.rendererOf(entry.effect());
            if (isOrbitingRenderer(renderer)) {
                renderer.renderEffect(entry.amplifier(), livingEntity, tickDelta, matrices, vertexConsumers, light);
            }
        }
    }
}
