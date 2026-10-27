package local.prominence.join.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.List;
import local.prominence.join.MaterialMapCapacity;
import net.irisshaders.iris.shaderpack.materialmap.BlockEntry;
import net.irisshaders.iris.shaderpack.materialmap.TagEntry;
import net.minecraft.class_2680;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets="net.irisshaders.iris.shaderpack.materialmap.BlockMaterialMapping", remap=false)
public abstract class IrisMaterialCapacityMixin {
    @WrapOperation(method="createBlockStateIdMap", at=@At(value="NEW", target="()Lit/unimi/dsi/fastutil/objects/Object2IntLinkedOpenHashMap;"), remap=false)
    private static Object2IntLinkedOpenHashMap<class_2680> prominence$sizeFromLastBuild(Operation<Object2IntLinkedOpenHashMap<class_2680>> original) {
        int size=MaterialMapCapacity.expected();
        return size==16?original.call():new Object2IntLinkedOpenHashMap<>(size);
    }
    @Inject(method="createBlockStateIdMap", at=@At("RETURN"), remap=false, require=1)
    private static void prominence$rememberSize(Int2ObjectLinkedOpenHashMap<List<BlockEntry>> blocks, Int2ObjectLinkedOpenHashMap<List<TagEntry>> tags, CallbackInfoReturnable<Object2IntMap<class_2680>> ci) {
        MaterialMapCapacity.completed(ci.getReturnValue().size());
    }
}
