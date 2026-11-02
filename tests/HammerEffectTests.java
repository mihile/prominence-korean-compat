package local.prominence.hammereffect;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.class_7833;
import org.joml.Quaternionf;

public final class HammerEffectTests {
    private static int checks;

    private static void check(boolean ok, String msg) {
        if (!ok) throw new AssertionError("HammerEffectTests failed (" + checks + "): " + msg);
        checks++;
    }

    public static void main(String[] args) throws Exception {
        long serverTicks = 28_724_252L; // > 2^24 ticks where float32 ulp(ticks) >= 2.0f
        float hammerSpeed = 9.0f;

        // 1. Verify sub-tick smoothness at > 2^24 ticks across 60 FPS within a tick
        float prevAngle = OrbitEffectMath.wrappedAngle(serverTicks, 0.0f, hammerSpeed, -45.0f);
        check(prevAngle >= 0.0f && prevAngle < 360.0f, "initial wrapped angle in [0,360)");
        Quaternionf prevQuat = class_7833.field_40716.rotationDegrees(prevAngle);
        for (int frame = 1; frame <= 60; frame++) {
            float delta = frame / 60.0f;
            float angle = OrbitEffectMath.wrappedAngle(serverTicks, delta, hammerSpeed, -45.0f);
            check(angle >= 0.0f && angle < 360.0f, "wrapped angle in [0,360)");
            float step = angle - prevAngle;
            if (step < -180.0f) step += 360.0f;
            check(Math.abs(step - (hammerSpeed / 60.0f)) < 1e-3f, "smooth sub-tick step=" + step);
            Quaternionf q = class_7833.field_40716.rotationDegrees(angle);
            float dot = Math.abs(prevQuat.x * q.x + prevQuat.y * q.y + prevQuat.z * q.z + prevQuat.w * q.w);
            check(dot < 0.9999999f, "quaternion advances every frame");
            prevAngle = angle;
            prevQuat = q;
        }

        // 2. Verify tick-to-tick continuity across odd and even ticks (where float32 rounds odd ticks to even)
        float endOfTick = OrbitEffectMath.wrappedAngle(serverTicks, 1.0f, hammerSpeed, -45.0f);
        float nextTickStart = OrbitEffectMath.wrappedAngle(serverTicks + 1L, 0.0f, hammerSpeed, -45.0f);
        float boundaryDiff = Math.abs(endOfTick - nextTickStart);
        if (boundaryDiff > 180.0f) boundaryDiff = 360.0f - boundaryDiff;
        check(boundaryDiff < 1e-3f, "continuous across tick boundary: " + boundaryDiff);

        // 3. Verify all effect speeds (BoneArmor 0.7f, SpellEngine 2.25f, Overlay 4.0f, SelfRotation 10.0f, negative)
        for (float speed : new float[]{0.7f, 2.25f, 4.0f, 9.0f, 10.0f, 16.25f, -9.0f}) {
            float a0 = OrbitEffectMath.wrappedAngle(serverTicks, 0.25f, speed, -45.0f);
            float a1 = OrbitEffectMath.wrappedAngle(serverTicks, 0.75f, speed, -45.0f);
            check(a0 >= 0.0f && a0 < 360.0f && a1 >= 0.0f && a1 < 360.0f, "range for speed " + speed);
            float expectedDelta = 0.5f * speed;
            float actualDelta = a1 - a0;
            if (actualDelta < -180.0f) actualDelta += 360.0f;
            if (actualDelta > 180.0f) actualDelta -= 360.0f;
            check(Math.abs(actualDelta - expectedDelta) < 1e-3f, "half-tick delta for speed " + speed);
        }

        // 4. First-person pass gating
        check(OrbitEffectMath.shouldRenderFirstPersonPass(false, true, false, false), "1st-person focused active player renders");
        check(!OrbitEffectMath.shouldRenderFirstPersonPass(true, true, false, false), "3rd-person skipped (already rendered by entity loop)");
        check(!OrbitEffectMath.shouldRenderFirstPersonPass(false, false, false, false), "non-focused entity skipped");
        check(!OrbitEffectMath.shouldRenderFirstPersonPass(false, true, true, false), "spectator skipped");
        check(!OrbitEffectMath.shouldRenderFirstPersonPass(false, true, false, true), "sleeping skipped (already rendered by entity loop)");

        // 5. Pin file structure & guard rejection on mismatch
        try (var in = HammerEffectGuard.class.getResourceAsStream("/prominence-hammer-effect-pins.json")) {
            check(in != null, "pins resource present");
            var pins = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            check(pins.has("simplyskills") && pins.has("spell_engine") && pins.has("minecraft"), "expected mod keys");
            Map<String, String> versions = new HashMap<>();
            versions.put("simplyskills", "1.7.2+1.20.1");
            versions.put("spell_engine", "0.15.12+1.20.1");
            versions.put("minecraft", "1.20.1");
            check(!HammerEffectGuard.matchesPins(pins, versions::get, (id, path) -> new byte[]{1, 2, 3}),
                "tampered bytecode rejected by guard");
        }

        System.out.println("PASS: hammer effect precision & first-person visibility checks=" + checks + "; no game/GL");
    }
}
