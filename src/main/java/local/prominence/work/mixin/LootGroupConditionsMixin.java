package local.prominence.work.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.llamalad7.mixinextras.sugar.Local;
import dev.emi.emi.api.widget.SlotWidget;
import fzzyhmstrs.emi_loot.util.ConditionalStack;
import net.minecraft.class_2561;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import dev.emi.emi.api.recipe.EmiRecipe;
import local.prominence.work.LootDropTooltip;

@Mixin(targets="fzzyhmstrs.emi_loot.util.IconGroupEmiWidget",remap=false)
public abstract class LootGroupConditionsMixin {
    @WrapOperation(method="<init>",at=@At(value="INVOKE",target="Ldev/emi/emi/api/widget/SlotWidget;appendTooltip(Lnet/minecraft/class_2561;)Ldev/emi/emi/api/widget/SlotWidget;"))
    private SlotWidget prominence$showConditionsOnItem(SlotWidget slot,class_2561 text,Operation<SlotWidget> original,@Local ConditionalStack entry,@Local(argsOnly=true) EmiRecipe recipe){
        return LootDropTooltip.append(slot,entry,recipe);
    }
}
