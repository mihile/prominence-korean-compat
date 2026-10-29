package local.prominence.work.mixin;
import local.prominence.work.*;import net.minecraft.*;import org.spongepowered.asm.mixin.Mixin;import org.spongepowered.asm.mixin.injection.At;import com.llamalad7.mixinextras.injector.wrapoperation.*;
@Mixin(targets={"fzzyhmstrs.emi_loot.emi.ChestLootRecipe","fzzyhmstrs.emi_loot.emi.ArchaeologyLootRecipe","fzzyhmstrs.emi_loot.util.IconGroupEmiWidget"},remap=false) public abstract class LootRollStaticMixin {
 @WrapOperation(method={"addWidgets","<init>"},at=@At(value="INVOKE",target="Lfzzyhmstrs/emi_loot/util/FloatTrimmer;trimFloatString(FI)Ljava/lang/String;")) private String precision(float value,int places,Operation<String> original){return LootNumbers.precise(value);}
 @WrapOperation(method={"addWidgets","<init>"},at=@At(value="INVOKE",target="Ljava/lang/Math;max(FF)F"),require=0) private float unclamp(float value,float floor,Operation<Float> original){return floor==0.01f?value:original.call(value,floor);}
 @WrapOperation(method={"addWidgets","<init>"},at=@At(value="INVOKE",target="Lfzzyhmstrs/emi_loot/util/LText;translatable(Ljava/lang/String;[Ljava/lang/Object;)Lnet/minecraft/class_5250;")) private class_5250 tooltip(String key,Object[] args,Operation<class_5250> original){if(key.equals("emi_loot.rolls")||key.equals("emi_loot.percentage")){var t=LootUiText.expectation(key,args);if(t!=null)return t;}return original.call(key,args);}
}
