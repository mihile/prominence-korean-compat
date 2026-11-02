package local.prominence.hammereffect.mixin;

import local.prominence.hammereffect.OrbitEffectMath;
import net.minecraft.class_1309;
import net.minecraft.class_4587;
import net.minecraft.class_4597;
import net.minecraft.class_922;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = class_922.class, remap = false)
public abstract class FirstPersonShadowOrbitEffectMixin {
    @Inject(
        method = "method_4054(Lnet/minecraft/class_1309;FFLnet/minecraft/class_4587;Lnet/minecraft/class_4597;I)V",
        at = @At("TAIL"),
        remap = false
    )
    private void prominence$renderFirstPersonShadowOrbitingEffects(class_1309 livingEntity, float yaw, float tickDelta,
                                                                   class_4587 matrixStack, class_4597 vertexConsumers,
                                                                   int light, CallbackInfo ci) {
        OrbitEffectMath.renderShadowPassFirstPersonEffects(livingEntity, tickDelta, matrixStack, vertexConsumers, light);
    }
}
