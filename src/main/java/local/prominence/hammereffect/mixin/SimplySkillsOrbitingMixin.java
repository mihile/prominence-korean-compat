package local.prominence.hammereffect.mixin;

import local.prominence.hammereffect.OrbitEffectMath;
import net.minecraft.class_1309;
import net.sweenus.simplyskills.client.effects.OrbitingEffectRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = OrbitingEffectRenderer.class, remap = false)
public abstract class SimplySkillsOrbitingMixin {
    @Shadow(remap = false)
    public float speed;

    @ModifyVariable(
        method = "renderEffect(ILnet/minecraft/class_1309;FLnet/minecraft/class_4587;Lnet/minecraft/class_4597;I)V",
        at = @At(value = "STORE", ordinal = 0),
        index = 8,
        remap = false
    )
    private float prominence$wrapOrbitAngle(float originalAngle, int amplifier, class_1309 livingEntity, float delta) {
        if (livingEntity == null || livingEntity.method_37908() == null) {
            return originalAngle;
        }
        return OrbitEffectMath.wrappedAngle(livingEntity.method_37908().method_8510(), delta, this.speed, -45.0f);
    }
}
