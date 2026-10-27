package local.prominence.work.mixin;

import appeng.items.parts.FacadeItem;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import local.prominence.work.FacadeBuildScope;
import net.minecraft.class_1747;
import net.minecraft.class_1761;
import net.minecraft.class_1799;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets="appeng.core.FacadeCreativeTab", remap=false)
public abstract class FacadeCreativeBuildMixin {
    @WrapMethod(method="buildDisplayItems", remap=false)
    private static void prominence$oneBuild(class_1761.class_8128 context, class_1761.class_7704 output,
                                            Operation<Void> original) {
        try (var scope = FacadeBuildScope.begin()) {
            try { original.call(context, output); }
            finally {
                LoggerFactory.getLogger("Prominence compatibility").info(
                    "AE2 creative facades: candidates={}, repeated={}, elapsedMs={}",
                    scope.candidates(), scope.repeated(), scope.elapsedMillis());
            }
        }
    }

    @WrapOperation(method="buildDisplayItems", at=@At(value="INVOKE",
        target="Lappeng/items/parts/FacadeItem;createFacadeForItem(Lnet/minecraft/class_1799;Z)Lnet/minecraft/class_1799;"), remap=false)
    private static class_1799 prominence$oncePerPlainBlock(FacadeItem facade, class_1799 input,
                                                          boolean testOnly, Operation<class_1799> original) {
        // Native AE2 accepts only block items without NBT and creates one facade per item ID.
        // Do not speculate about custom ItemStack/BlockItem subclasses or test-only return semantics.
        if (testOnly || ((Object)facade).getClass() != FacadeItem.class || input.getClass() != class_1799.class || input.method_7960()
            || input.method_7985() || input.method_7909().getClass() != class_1747.class) {
            return original.call(facade, input, testOnly);
        }
        return FacadeBuildScope.first(input.method_7909(),
            () -> original.call(facade, input, testOnly), class_1799.field_8037);
    }
}
