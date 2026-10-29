package local.prominence.work.mixin;
import java.util.concurrent.*;
import local.prominence.work.TrimPreviewAssets;
import net.minecraft.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets="net.minecraft.class_1092",remap=false)
public abstract class TrimPreviewReloadMixin {
    @Inject(method="method_25931",at=@At("HEAD"),remap=false,require=1)
    private void prominence$resources(class_3302.class_4045 sync,class_3300 manager,class_3695 prepare,class_3695 apply,Executor prepareExecutor,Executor applyExecutor,CallbackInfoReturnable<CompletableFuture<Void>> ci){
        TrimPreviewAssets.beginReload(manager);
    }
}
