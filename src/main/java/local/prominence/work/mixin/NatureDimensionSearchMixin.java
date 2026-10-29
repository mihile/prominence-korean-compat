package local.prominence.work.mixin;
import local.prominence.work.LootUiText;import net.minecraft.*;import com.chaosthedude.naturescompass.NaturesCompass;import org.spongepowered.asm.mixin.Mixin;import org.spongepowered.asm.mixin.injection.At;import com.llamalad7.mixinextras.injector.wrapoperation.*;
@Mixin(targets="com.chaosthedude.naturescompass.gui.NaturesCompassScreen",remap=false) public abstract class NatureDimensionSearchMixin {
 @WrapOperation(method="processSearchTerm",at=@At(value="INVOKE",target="Lcom/chaosthedude/naturescompass/utils/BiomeUtils;getBiomeNameForDisplay(Lnet/minecraft/class_1937;Lnet/minecraft/class_2960;)Ljava/lang/String;")) private String search(class_1937 world,class_2960 id,Operation<String> original){return LootUiText.dimensions(original.call(world,id),id,NaturesCompass.dimensionIDsForAllowedBiomeIDs.get(id));}
 @WrapOperation(method="processSearchTerm",at=@At(value="INVOKE",target="Ljava/lang/String;toLowerCase()Ljava/lang/String;"),require=4) private String lower(String s,Operation<String> original){return s.toLowerCase(java.util.Locale.ROOT);}
}
