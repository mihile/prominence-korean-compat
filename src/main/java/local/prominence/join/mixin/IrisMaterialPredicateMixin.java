package local.prominence.join.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.google.common.collect.ImmutableList;
import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.*;
import local.prominence.join.MaterialPredicateScope;
import net.irisshaders.iris.shaderpack.materialmap.*;
import net.minecraft.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets="net.irisshaders.iris.shaderpack.materialmap.BlockMaterialMapping",remap=false)
public abstract class IrisMaterialPredicateMixin {
 @WrapMethod(method="createBlockStateIdMap",remap=false)
 private static Object2IntMap<class_2680> prominence$materialScope(Int2ObjectLinkedOpenHashMap<List<BlockEntry>> blocks,Int2ObjectLinkedOpenHashMap<List<TagEntry>> tags,Operation<Object2IntMap<class_2680>> original){
  try(var scope=MaterialPredicateScope.begin()){return original.call(blocks,tags);}
 }
 @WrapOperation(method={"addBlockStates","lambda$addTag$4"},at=@At(value="INVOKE",target="Lnet/minecraft/class_2689;method_11662()Lcom/google/common/collect/ImmutableList;",ordinal=0),remap=false)
 private static ImmutableList<class_2680> prominence$alreadyAssigned(class_2689<?,?> manager,Operation<ImmutableList<class_2680>> original){return MaterialPredicateScope.unconditional(original.call(manager));}
 @WrapOperation(method={"addBlockStates","lambda$addTag$4"},at=@At(value="INVOKE",target="Lnet/minecraft/class_2689;method_11662()Lcom/google/common/collect/ImmutableList;",ordinal=1),remap=false)
 private static ImmutableList<class_2680> prominence$selectiveStates(class_2689<?,?> manager,Operation<ImmutableList<class_2680>> original,@Local(ordinal=1) Map<class_2769<?>,String> conditions){
  return MaterialPredicateScope.filter(original.call(manager),conditions);
 }
}
