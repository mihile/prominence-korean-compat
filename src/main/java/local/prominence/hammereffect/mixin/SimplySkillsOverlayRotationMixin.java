package local.prominence.hammereffect.mixin;

import local.prominence.hammereffect.OrbitEffectMath;
import net.minecraft.class_1309;
import net.minecraft.class_4587;
import net.minecraft.class_4597;
import net.minecraft.class_7833;
import net.sweenus.simplyskills.client.effects.DeathMarkRenderer;
import net.sweenus.simplyskills.client.effects.MagicCircleRenderer;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = {DeathMarkRenderer.class, MagicCircleRenderer.class}, remap = false)
public abstract class SimplySkillsOverlayRotationMixin {
    @Redirect(
        method = "renderEffect(ILnet/minecraft/class_1309;FLnet/minecraft/class_4587;Lnet/minecraft/class_4597;I)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/class_7833;rotationDegrees(F)Lorg/joml/Quaternionf;"
        ),
        remap = false
    )
    private Quaternionf prominence$wrapOverlayRotation(class_7833 axis, float originalDegrees, int amplifier,
                                                       class_1309 livingEntity, float delta,
                                                       class_4587 matrixStack, class_4597 vertexConsumers, int light) {
        if (livingEntity == null || livingEntity.method_37908() == null) {
            return axis.rotationDegrees(originalDegrees);
        }
        return axis.rotationDegrees(OrbitEffectMath.wrappedAngle(
            livingEntity.method_37908().method_8510(), delta, 4.0f, -45.0f));
    }
}
