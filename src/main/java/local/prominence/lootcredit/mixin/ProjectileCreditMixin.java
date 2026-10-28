package local.prominence.lootcredit.mixin;
import local.prominence.lootcredit.*;
import net.minecraft.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(class_1676.class)
public abstract class ProjectileCreditMixin implements CreditHolder {
 @Unique private Credit credit$value;
 public Credit prominence$credit(){return credit$value;}public void prominence$credit(Credit c){credit$value=c;}
 @Inject(method="method_7432",at=@At("TAIL")) private void credit$owner(class_1297 owner,CallbackInfo ci){
  class_1799 stack=owner instanceof class_1309 l&&!l.method_6030().method_7960()?l.method_6030():owner instanceof class_1309 l?l.method_6047():class_1799.field_8037;
  credit$value=Credit.capture(owner,stack);
 }
 @Inject(method="method_5652",at=@At("TAIL")) private void credit$save(class_2487 tag,CallbackInfo ci){if(credit$value!=null)credit$value.write(tag);}
 @Inject(method="method_5749",at=@At("TAIL")) private void credit$load(class_2487 tag,CallbackInfo ci){credit$value=Credit.read(tag);}
}
