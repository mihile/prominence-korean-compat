package local.prominence.lootcredit.mixin;
import local.prominence.lootcredit.*;import net.minecraft.*;import net.soulsweaponry.entitydata.IEntityDataSaver;import org.spongepowered.asm.mixin.Mixin;import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
@Mixin(targets="net.soulsweaponry.api.entitystats.EntityBleed",remap=false) public abstract class BloodLossCreditMixin {
 @WrapMethod(method="triggerBloodLoss") private static void credit$loss(class_1309 entity,Operation<Void> original){var data=((IEntityDataSaver)entity).getPersistentData();var c=data.method_10545("ProminenceBleedLoot")?Credit.read(data.method_10562("ProminenceBleedLoot")):null;try(var scope=CreditScope.enter(c,true)){original.call(entity);}}
}
