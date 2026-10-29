package local.prominence.lootcredit.mixin;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import local.prominence.lootcredit.*;
import net.minecraft.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
@Mixin(class_1890.class)
public abstract class LootingCreditMixin {
 @ModifyReturnValue(method="method_8226",at=@At("RETURN")) private static int credit$looting(int original,class_1309 actor){var c=LootCredit.dropCredit(actor);return c==null?original:Math.max(0,c.looting()+original-class_1890.method_8225(class_1893.field_9110,actor.method_6047()));}
}
