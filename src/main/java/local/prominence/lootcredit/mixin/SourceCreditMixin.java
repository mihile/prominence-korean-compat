package local.prominence.lootcredit.mixin;
import local.prominence.lootcredit.*;import net.minecraft.class_1282;import org.spongepowered.asm.mixin.*;
@Mixin(class_1282.class) public abstract class SourceCreditMixin implements CreditHolder {
 @Unique private Credit credit$value;public Credit prominence$credit(){return credit$value;}public void prominence$credit(Credit c){credit$value=c;}
}
