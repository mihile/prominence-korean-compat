package local.prominence.work.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Consumer;
import local.prominence.work.FtbQuestsFallbackStacks;
import net.minecraft.class_1761;
import net.minecraft.class_1799;
import net.minecraft.class_310;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Eliminates 307ms+ render thread freezes caused by FTB Quests synchronously rebuilding all creative tabs
 * while background creative reloading holds the ItemGroup monitor lock.
 */
@Mixin(targets="dev.ftb.mods.ftbquests.integration.item_filtering.DisplayStacksCache", remap=false)
public abstract class FtbQuestsDisplayStacksCacheMixin {

    @WrapOperation(
        method="computeMatchingStacks",
        at=@At(
            value="INVOKE",
            target="Ljava/util/Optional;ifPresent(Ljava/util/function/Consumer;)V"
        ),
        remap=false
    )
    private static void prominence$preventCreativeBuildStall(
            Optional<?> optional,
            Consumer<?> action,
            Operation<Void> original) {
        // When invoked from the render thread (e.g. inventory close or quest GUI open),
        // NEVER synchronously trigger full creative tab rebuilds.
        class_310 client = class_310.method_1551();
        if (Thread.currentThread().getName().equals("Render thread") || (client != null && client.method_18854())) {
            return;
        }
        original.call(optional, action);
    }

    @WrapOperation(
        method="computeMatchingStacks",
        at=@At(
            value="INVOKE",
            target="Lnet/minecraft/class_1761;method_45414()Ljava/util/Collection;"
        ),
        remap=false
    )
    private static Collection<class_1799> prominence$safeSearchStacks(
            class_1761 group,
            Operation<Collection<class_1799>> original) {
        Collection<class_1799> stacks = original.call(group);
        if (stacks != null && !stacks.isEmpty()) {
            return stacks;
        }
        return FtbQuestsFallbackStacks.getRegistryStacks();
    }
}
