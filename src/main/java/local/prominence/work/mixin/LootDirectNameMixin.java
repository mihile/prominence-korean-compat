package local.prominence.work.mixin;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;import fzzyhmstrs.emi_loot.emi.MobLootRecipe.MobLootRecipeData;import fzzyhmstrs.emi_loot.util.TrimmedTitle;import net.minecraft.*;import local.prominence.work.PlainBossName;import org.spongepowered.asm.mixin.Mixin;import org.spongepowered.asm.mixin.injection.At;
@Mixin(value=MobLootRecipeData.class,remap=false) public abstract class LootDirectNameMixin {
 @ModifyReturnValue(method="of",at=@At("RETURN")) private static MobLootRecipeData prominence$bossName(MobLootRecipeData data){
  if(data.entity()!=null){var original=data.entity().method_5477();var name=PlainBossName.get(data.loot().mobId,original);if(name!=original)return new MobLootRecipeData(data.loot(),data.spawnEggItem(),data.entity(),data.scale(),TrimmedTitle.of(name,108));return data;}
  if(!data.loot().mobId.toString().equals("eldritch_end:the_faceless"))return data;
  var type=class_7923.field_41177.method_10223(data.loot().mobId);if(type==null)return data;
  return new MobLootRecipeData(data.loot(),data.spawnEggItem(),null,data.scale(),TrimmedTitle.of(type.method_5897(),108));
 }
}
