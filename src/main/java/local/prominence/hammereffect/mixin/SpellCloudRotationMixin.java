package local.prominence.hammereffect.mixin;

import local.prominence.hammereffect.OrbitEffectMath;
import net.minecraft.class_4587;
import net.minecraft.class_4597;
import net.minecraft.class_7833;
import net.spell_engine.client.render.SpellCloudRenderer;
import net.spell_engine.entity.SpellCloud;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = SpellCloudRenderer.class, remap = false)
public abstract class SpellCloudRotationMixin {
    @Redirect(
        method = "render(Lnet/spell_engine/entity/SpellCloud;FFLnet/minecraft/class_4587;Lnet/minecraft/class_4597;I)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/class_7833;rotationDegrees(F)Lorg/joml/Quaternionf;",
            ordinal = 1
        ),
        remap = false
    )
    private Quaternionf prominence$wrapCloudRotation(class_7833 axis, float originalDegrees, SpellCloud entity,
                                                     float yaw, float tickDelta, class_4587 matrixStack,
                                                     class_4597 vertexConsumers, int light) {
        if (entity == null || entity.method_37908() == null) {
            return axis.rotationDegrees(originalDegrees);
        }
        var data = entity.getCloudData();
        if (data == null || data.client_data == null || data.client_data.model == null) {
            return axis.rotationDegrees(originalDegrees);
        }
        float speed = data.client_data.model.rotate_degrees_per_tick;
        return axis.rotationDegrees(OrbitEffectMath.wrappedAngle(
            entity.method_37908().method_8510(), tickDelta, speed, 0.0f));
    }
}
