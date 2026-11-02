package local.prominence.hammereffect.mixin;

import local.prominence.hammereffect.OrbitEffectMath;
import net.minecraft.class_310;
import net.minecraft.class_4184;
import net.minecraft.class_4587;
import net.minecraft.class_4599;
import net.minecraft.class_757;
import net.minecraft.class_761;
import net.minecraft.class_765;
import net.minecraft.class_898;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = class_761.class, remap = false)
public abstract class FirstPersonOrbitEffectWorldMixin {
    @Shadow(remap = false)
    @Final
    private class_310 field_4088;

    @Shadow(remap = false)
    @Final
    private class_898 field_4109;

    @Shadow(remap = false)
    @Final
    private class_4599 field_20951;

    @Inject(
        method = "method_22710(Lnet/minecraft/class_4587;FJZLnet/minecraft/class_4184;Lnet/minecraft/class_757;Lnet/minecraft/class_765;Lorg/joml/Matrix4f;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/class_761;method_22979(Lnet/minecraft/class_4587;)V",
            ordinal = 0
        ),
        remap = false
    )
    private void prominence$renderFirstPersonOrbitingEffects(class_4587 matrices, float tickDelta, long limitTime,
                                                             boolean renderBlockOutline, class_4184 camera,
                                                             class_757 gameRenderer, class_765 lightmapTextureManager,
                                                             Matrix4f projectionMatrix, CallbackInfo ci) {
        if (this.field_20951 == null) {
            return;
        }
        OrbitEffectMath.renderFirstPersonOrbitingEffects(
            this.field_4088,
            camera,
            this.field_4109,
            tickDelta,
            matrices,
            this.field_20951.method_23000()
        );
    }
}
