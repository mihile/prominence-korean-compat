package local.prominence.work.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import net.minecraft.class_2561;
import net.minecraft.class_5250;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import fzzyhmstrs.emi_loot.EMILoot;

@Mixin(targets={"fzzyhmstrs.emi_loot.emi.MobLootRecipe","fzzyhmstrs.emi_loot.emi.BlockLootRecipe","fzzyhmstrs.emi_loot.emi.GameplayLootRecipe"},remap=false)
public abstract class LootConditionsVisibleMixin {
    @WrapOperation(method="addWidgets",at=@At(value="INVOKE",target="Lfzzyhmstrs/emi_loot/EMILoot$EmiLootConfig;isNotPlain()Z"))
    private boolean prominence$keepRequiredConditions(EMILoot.EmiLootConfig config,Operation<Boolean> original){return true;}
    @WrapOperation(method="addWidgets",at=@At(value="INVOKE",target="Lfzzyhmstrs/emi_loot/util/SymbolText;of(ILnet/minecraft/class_2561;)Lnet/minecraft/class_5250;"))
    private class_5250 prominence$plainConditions(int symbol,class_2561 text,Operation<class_5250> original){return text.method_27661();}
}
