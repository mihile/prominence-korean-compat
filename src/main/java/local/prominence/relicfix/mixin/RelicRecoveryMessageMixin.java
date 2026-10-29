package local.prominence.relicfix.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.class_1661;
import net.minecraft.class_1799;
import net.minecraft.class_3222;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets="elocindev.prominent.recovery.RelicRecoveryManager", remap=false)
public abstract class RelicRecoveryMessageMixin {
    @WrapOperation(method="grantOrFlagRelic", at=@At(value="INVOKE", target="Lnet/minecraft/class_1661;method_7394(Lnet/minecraft/class_1799;)Z"), remap=false)
    private static boolean prominence$rememberRelic(class_1661 inventory, class_1799 stack, Operation<Boolean> original,
            @Share("relicNameStack") LocalRef<class_1799> saved) {
        saved.set(stack.method_7972());
        return original.call(inventory, stack);
    }
    @WrapOperation(method="grantOrFlagRelic", at=@At(value="INVOKE", target="Lelocindev/prominent/recovery/RelicRecoveryManager;sendOwnershipMessage(Lnet/minecraft/class_3222;Lnet/minecraft/class_1799;)V"), remap=false)
    private static void prominence$displayOriginalRelic(class_3222 player, class_1799 stack, Operation<Void> original,
            @Share("relicNameStack") LocalRef<class_1799> saved) {
        class_1799 snapshot=saved.get();
        original.call(player, stack.method_7960() && snapshot != null ? snapshot : stack);
    }
}
