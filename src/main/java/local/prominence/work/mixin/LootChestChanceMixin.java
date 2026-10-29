package local.prominence.work.mixin;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.llamalad7.mixinextras.sugar.Local;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.widget.SlotWidget;
import local.prominence.work.LootDropTooltip;
import net.minecraft.class_2561;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
@Mixin(targets={"fzzyhmstrs.emi_loot.emi.ChestLootRecipe","fzzyhmstrs.emi_loot.emi.ArchaeologyLootRecipe"},remap=false)
public abstract class LootChestChanceMixin {
 @WrapOperation(method="addWidgets",at=@At(value="INVOKE",target="Ldev/emi/emi/api/widget/SlotWidget;appendTooltip(Lnet/minecraft/class_2561;)Ldev/emi/emi/api/widget/SlotWidget;"))
 private SlotWidget prominence$actualChance(SlotWidget slot,class_2561 ignored,Operation<SlotWidget> original,@Local float weight){return LootDropTooltip.appendChest(slot,weight,(EmiRecipe)(Object)this);}
}
