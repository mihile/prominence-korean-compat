package local.prominence.memory.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import local.prominence.memory.BackpackUnloadCallbacks;
import local.prominence.memory.BackpackUnloadDispatcher;
import net.fabricmc.fabric.api.event.Event;
import net.minecraft.class_2586;
import net.minecraft.class_2818;
import net.minecraft.class_3218;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets="net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackBlockEntity",remap=false)
public abstract class BackpackWeakUnloadMixin implements BackpackUnloadCallbacks {
    @Invoker("lambda$new$0") public abstract void prominence$nativeChunkUnload(class_3218 world,class_2818 chunk);
    @Invoker("lambda$new$1") public abstract void prominence$nativeBlockEntityUnload(class_2586 entity,class_3218 world);

    @WrapOperation(method="<init>",at=@At(value="INVOKE",target="Lnet/fabricmc/fabric/api/event/Event;register(Ljava/lang/Object;)V"),remap=false,require=2,expect=2,allow=2)
    private void prominence$useWeakUnloadRegistration(Event<?> event,Object callback,Operation<Void> original) {
        // The original per-instance lambda is not kept. Dispatch invokes the exact native body.
        BackpackUnloadDispatcher.track(this);
    }
}
