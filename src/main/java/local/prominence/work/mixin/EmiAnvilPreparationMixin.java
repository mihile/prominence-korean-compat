package local.prominence.work.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.emi.emi.api.stack.EmiStack;
import local.prominence.work.EmiStackBuildScope;
import net.minecraft.class_1792;
import net.minecraft.class_1935;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "dev.emi.emi.recipe.special.EmiAnvilEnchantRecipe", remap = false)
public abstract class EmiAnvilPreparationMixin {
    @Shadow @Final private class_1792 tool;

    @WrapOperation(method = {"getInputs", "getOutputs"}, at = @At(value = "INVOKE",
        target = "Ldev/emi/emi/api/stack/EmiStack;of(Lnet/minecraft/class_1935;)Ldev/emi/emi/api/stack/EmiStack;"), remap = false)
    private EmiStack prominence$preparePlainTool(class_1935 item, Operation<EmiStack> original) {
        return EmiStackBuildScope.item(item, () -> original.call(item));
    }

    @WrapMethod(method = "getBook", remap = false)
    private EmiStack prominence$prepareBook(Operation<EmiStack> original) {
        return EmiStackBuildScope.book(this, tool, () -> original.call());
    }
}
