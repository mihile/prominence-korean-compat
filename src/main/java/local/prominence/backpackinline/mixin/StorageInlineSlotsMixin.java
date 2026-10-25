package local.prominence.backpackinline.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.List;
import local.prominence.backpackinline.InlineReforge;
import net.minecraft.class_1657;
import net.minecraft.class_1735;
import net.minecraft.class_1799;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value=StorageContainerMenuBase.class,remap=false)
public abstract class StorageInlineSlotsMixin {
    @WrapOperation(method="lambda$mergeStackToOpenUpgradeTab$14",at=@At(value="INVOKE",target="Lnet/p3pp3rf1y/sophisticatedcore/common/gui/UpgradeContainerBase;getSlots()Ljava/util/List;"))
    private List<class_1735> prominence$activeTransfer(UpgradeContainerBase<?,?> container,Operation<List<class_1735>> original){var state=InlineReforge.state(container);return state==null?original.call(container):state.activeSlots();}
    @Inject(method="method_7601",at=@At("HEAD"),cancellable=true)
    private void prominence$quickMove(class_1657 player,int index,CallbackInfoReturnable<class_1799> cir){if(InlineReforge.blocked(this,index))cir.setReturnValue(class_1799.field_8037);}
    @Inject(method="method_7613",at=@At("HEAD"),cancellable=true)
    private void prominence$pickupAll(class_1799 stack,class_1735 slot,CallbackInfoReturnable<Boolean> cir){if(InlineReforge.blocked(this,slot))cir.setReturnValue(false);}
    @Inject(method="method_7623",at=@At("TAIL"))
    private void prominence$poll(CallbackInfo ci){InlineReforge.poll(this);}
}
