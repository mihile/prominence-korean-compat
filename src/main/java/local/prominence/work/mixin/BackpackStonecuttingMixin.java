package local.prominence.work.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.handler.EmiCraftContext;
import java.util.List;
import local.prominence.work.BackpackStonecutting;
import net.minecraft.*;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import net.p3pp3rf1y.sophisticatedcore.compat.emi.EmiGridMenuInfo;
import org.spongepowered.asm.mixin.*;

@Mixin(value=EmiGridMenuInfo.class,remap=false)
public abstract class BackpackStonecuttingMixin {
    @Shadow @Final private class_3956<?> recipeType;
    @WrapMethod(method="supportsRecipe")
    private boolean prominence$supports(EmiRecipe r,Operation<Boolean> original) {
        return recipeType==class_3956.field_17641?BackpackStonecutting.INSTANCE.supportsRecipe(r):original.call(r);
    }
    @WrapMethod(method="getInputSources(Lnet/p3pp3rf1y/sophisticatedcore/common/gui/StorageContainerMenuBase;)Ljava/util/List;")
    private List<class_1735> prominence$sources(StorageContainerMenuBase<?> m,Operation<List<class_1735>> original) {
        return recipeType==class_3956.field_17641?BackpackStonecutting.INSTANCE.getInputSources(m):original.call(m);
    }
    @WrapMethod(method="getCraftingSlots(Lnet/p3pp3rf1y/sophisticatedcore/common/gui/StorageContainerMenuBase;)Ljava/util/List;")
    private List<class_1735> prominence$input(StorageContainerMenuBase<?> m,Operation<List<class_1735>> original) {
        return recipeType==class_3956.field_17641?BackpackStonecutting.INSTANCE.getCraftingSlots(m):original.call(m);
    }
    @WrapMethod(method="getOutputSlot(Lnet/p3pp3rf1y/sophisticatedcore/common/gui/StorageContainerMenuBase;)Lnet/minecraft/class_1735;")
    private class_1735 prominence$output(StorageContainerMenuBase<?> m,Operation<class_1735> original) {
        return recipeType==class_3956.field_17641?BackpackStonecutting.INSTANCE.getOutputSlot(m):original.call(m);
    }
    @WrapMethod(method="canCraft")
    private boolean prominence$can(EmiRecipe r,EmiCraftContext<StorageContainerMenuBase<?>> c,Operation<Boolean> original) {
        return recipeType==class_3956.field_17641?BackpackStonecutting.INSTANCE.canCraft(r,c):original.call(r,c);
    }
    @WrapMethod(method="craft")
    private boolean prominence$craft(EmiRecipe r,EmiCraftContext<StorageContainerMenuBase<?>> c,Operation<Boolean> original) {
        return recipeType==class_3956.field_17641?BackpackStonecutting.INSTANCE.craft(r,c):original.call(r,c);
    }
}
