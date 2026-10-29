package local.prominence.lootcredit.mixin;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import local.prominence.lootcredit.*;
import net.minecraft.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
@Mixin(targets="dev.shadowsoffire.apotheosis.ench.enchantments.masterwork.ScavengerEnchant",remap=false)
public abstract class ScavengerCreditMixin {
 @WrapOperation(method="lambda$drops$0",at=@At(value="INVOKE",target="Lnet/minecraft/class_1890;method_8225(Lnet/minecraft/class_1887;Lnet/minecraft/class_1799;)I"))
 private int credit$scavenger(class_1887 enchant,class_1799 stack,Operation<Integer> original,class_1309 target,class_1282 source,java.util.Collection<?> drops,int looting,boolean recent){var c=LootCredit.dropCredit(source.method_5529());return c==null?original.call(enchant,stack):c.scavenger();}
}
