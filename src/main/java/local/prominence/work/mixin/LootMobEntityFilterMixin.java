package local.prominence.work.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import fzzyhmstrs.emi_loot.client.ClientBuiltPool;
import fzzyhmstrs.emi_loot.emi.MobLootRecipe;
import java.util.List;
import local.prominence.work.LootEntityDisplay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value=MobLootRecipe.class,remap=false)
public abstract class LootMobEntityFilterMixin {
    @ModifyExpressionValue(method="<init>",at=@At(value="FIELD",target="Lfzzyhmstrs/emi_loot/client/ClientMobLootTable;builtItems:Ljava/util/List;"))
    private List<ClientBuiltPool> prominence$matchVictim(List<ClientBuiltPool> pools,@Local(argsOnly=true) MobLootRecipe.MobLootRecipeData data){
        return LootEntityDisplay.filter(pools,data.loot().mobId);
    }
}
