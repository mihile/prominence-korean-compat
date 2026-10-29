package local.prominence.work.mixin;
import local.prominence.work.LootUiText;import net.minecraft.*;import com.chaosthedude.explorerscompass.ExplorersCompass;import org.spongepowered.asm.mixin.Mixin;import org.spongepowered.asm.mixin.injection.At;import com.llamalad7.mixinextras.injector.wrapoperation.*;
@Mixin(targets="com.chaosthedude.explorerscompass.gui.ExplorersCompassScreen",remap=false) public abstract class ExplorerDimensionSearchMixin {
 @WrapOperation(method="processSearchTerm",at=@At(value="INVOKE",target="Lcom/chaosthedude/explorerscompass/util/StructureUtils;getStructureName(Lnet/minecraft/class_2960;)Ljava/lang/String;")) private String search(class_2960 id,Operation<String> original){return LootUiText.dimensions(original.call(id),id,ExplorersCompass.allowedStructureIDsToDimensionIDs.get(id));}
}
