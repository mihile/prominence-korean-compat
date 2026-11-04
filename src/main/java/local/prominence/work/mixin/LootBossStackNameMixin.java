package local.prominence.work.mixin;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;import local.prominence.work.PlainBossName;import net.minecraft.*;import org.spongepowered.asm.mixin.Mixin;import org.spongepowered.asm.mixin.Shadow;import org.spongepowered.asm.mixin.Final;import org.spongepowered.asm.mixin.injection.At;
@Mixin(targets="fzzyhmstrs.emi_loot.util.stack.EntityEmiStack",remap=false) public abstract class LootBossStackNameMixin {
 @Shadow @Final private class_1297 entity;
 @ModifyReturnValue(method="getName",at=@At("RETURN")) private class_2561 prominence$plainBossName(class_2561 name){return entity==null?name:PlainBossName.get(class_7923.field_41177.method_10221(entity.method_5864()),name);}
}
