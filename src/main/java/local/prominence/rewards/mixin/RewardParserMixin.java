package local.prominence.rewards.mixin;
import java.util.Map;import local.prominence.rewards.*;import net.minecraft.*;import org.spongepowered.asm.mixin.Mixin;import org.spongepowered.asm.mixin.injection.*;import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(targets="fzzyhmstrs.emi_loot.parser.LootTableParser",remap=false)
public abstract class RewardParserMixin {
 @Inject(method="parseLootTables",at=@At("TAIL"))
 private static void prominence$tables(class_60 manager,Map<class_8488<?>,?> tables,CallbackInfo ci){try{RewardRegistry.index(tables);}catch(RuntimeException e){RewardRegistry.disable();org.slf4j.LoggerFactory.getLogger("Prominence compatibility").warn("Rare reward indexing failed; original loot retained",e);}}
 @Inject(method="registerServer",at=@At("HEAD"))
 private void prominence$metadata(class_3222 player,CallbackInfo ci){RewardCommonInit.request(player.field_13987);}
}
