package local.prominence.hammereffect.mixin;

import local.prominence.hammereffect.OrbitEffectMath;
import net.minecraft.class_1309;
import net.minecraft.class_310;
import net.minecraft.class_4587;
import net.minecraft.class_4597;
import net.minecraft.class_918;
import net.sweenus.simplyskills.client.effects.OrbitingRotatingEffectRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = OrbitingRotatingEffectRenderer.class, remap = false)
public abstract class SimplySkillsOrbitingRotatingMixin {
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

    @ModifyVariable(
        method = "renderModel(Lnet/minecraft/class_4587;FFFFLnet/minecraft/class_918;Lnet/minecraft/class_4597;ILnet/minecraft/class_1309;)V",
        at = @At(value = "STORE", ordinal = 0),
        index = 11,
        remap = false
    )
    private float prominence$wrapSelfRotation(float originalSelfAngle, class_4587 matrixStack, float scale,
                                              float verticalOffset, float horizontalOffset, float rotation,
                                              class_918 itemRenderer, class_4597 vertexConsumers,
                                              int light, class_1309 livingEntity) {
        if (livingEntity == null) {
            return originalSelfAngle;
        }
        class_310 client = class_310.method_1551();
        float tickDelta = client != null ? client.method_1488() : 0.0f;
        return OrbitEffectMath.wrappedAngle(livingEntity.field_6012, tickDelta, 10.0f, 0.0f);
    }
}
