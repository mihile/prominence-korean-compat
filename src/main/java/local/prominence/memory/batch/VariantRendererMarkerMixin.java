package local.prominence.memory.batch;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
@Mixin(targets={"net.minecraft.class_929","net.minecraft.class_882","net.minecraft.class_884","net.minecraft.class_4606","net.minecraft.class_932","net.minecraft.class_941","net.minecraft.class_994","net.minecraft.class_946","net.minecraft.class_949","net.minecraft.class_969","net.minecraft.class_968"},priority=500,remap=false)
public abstract class VariantRendererMarkerMixin {
    @Unique private static void prominence$variantProjectionMarker(){}
}
