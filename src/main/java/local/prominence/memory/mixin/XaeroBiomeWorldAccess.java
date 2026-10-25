package local.prominence.memory.mixin;
import net.minecraft.class_1937;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(targets="xaero.common.minimap.write.biome.BiomeBlendCalculator",remap=false)
public interface XaeroBiomeWorldAccess {
    @Accessor("original") class_1937 prominence$world();
    @Accessor("original") void prominence$world(class_1937 world);
}
