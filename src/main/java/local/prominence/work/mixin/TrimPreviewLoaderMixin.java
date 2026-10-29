package local.prominence.work.mixin;
import java.util.List;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import local.prominence.work.TrimPreviewAssets;
import net.minecraft.class_2960;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.objectweb.asm.Opcodes;

@Mixin(targets="io.github.andrew6rant.dynamictrim.resource.DynamicTrimLoader",remap=false)
public abstract class TrimPreviewLoaderMixin {
    @WrapOperation(method="lambda$loadTrims$4",at=@At(value="FIELD",target="Lio/github/andrew6rant/dynamictrim/resource/TrimModelHelper;TEMPLATE_IDS:Ljava/util/List;",opcode=Opcodes.GETSTATIC),remap=false)
    private static List<class_2960> prominence$validModels(Operation<List<class_2960>> original){return TrimPreviewAssets.supported(original.call());}
}
