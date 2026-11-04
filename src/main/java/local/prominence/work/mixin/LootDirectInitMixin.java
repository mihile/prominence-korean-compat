package local.prominence.work.mixin;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;import fzzyhmstrs.emi_loot.client.LootReceiver;import java.util.List;import local.prominence.work.SpecialLootDisplay;import org.spongepowered.asm.mixin.Mixin;import org.spongepowered.asm.mixin.injection.At;
@Mixin(targets="fzzyhmstrs.emi_loot.emi.EmiClientPlugin",remap=false) public abstract class LootDirectInitMixin {
 @ModifyExpressionValue(method="lambda$initialize$6",at=@At(value="FIELD",target="Lfzzyhmstrs/emi_loot/client/ClientLootTables;mobLoots:Ljava/util/List;"))
 private static List<LootReceiver> prominence$directMobPages(List<LootReceiver> entries){return SpecialLootDisplay.addMissing(entries);}
}
