package local.prominence.work.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.emi.emi.api.recipe.EmiPlayerInventory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.config.SidebarType;
import dev.emi.emi.runtime.EmiSidebars;
import dev.emi.emi.screen.EmiScreenManager;
import java.util.List;
import local.prominence.work.CraftableRefresh;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value=EmiScreenManager.class,remap=false)
public abstract class EmiCraftableRefreshMixin {
    @Shadow private static EmiPlayerInventory lastPlayerInventory;
    @Unique private static final CraftableRefresh prominence$refresh=new CraftableRefresh();
    @Unique private static long prominence$lastSlowReport;
    @Unique private static long prominence$craftableNanos;

    @WrapMethod(method="updateCraftables",remap=false)
    private static void prominence$refreshWhenShown(Operation<Void> original) {
        if(prominence$refresh.needsRefresh(EmiScreenManager.hasSidebarVisible(SidebarType.CRAFTABLES)))lastPlayerInventory=null;
        long start=System.nanoTime();prominence$craftableNanos=0;
        try{original.call();}finally{
            long elapsed=System.nanoTime()-start;
            if(elapsed>=50_000_000L && start-prominence$lastSlowReport>=5_000_000_000L){
                prominence$lastSlowReport=start;
                LoggerFactory.getLogger("Prominence compatibility").info("Slow EMI inventory refresh: totalMs={}, craftablesMs={}, craftableTabVisible={}",elapsed/1_000_000L,prominence$craftableNanos/1_000_000L,EmiScreenManager.hasSidebarVisible(SidebarType.CRAFTABLES));
            }
        }
    }
    @WrapOperation(method="updateCraftables",at=@At(value="INVOKE",target="Ldev/emi/emi/api/recipe/EmiPlayerInventory;getCraftables()Ljava/util/List;"),remap=false)
    private static List<EmiIngredient> prominence$deferHiddenList(EmiPlayerInventory inventory,Operation<List<EmiIngredient>> original) {
        if(!prominence$refresh.shouldCompute(EmiScreenManager.hasSidebarVisible(SidebarType.CRAFTABLES)))return EmiSidebars.craftables;
        long start=System.nanoTime();try{return original.call(inventory);}finally{prominence$craftableNanos+=System.nanoTime()-start;}
    }
    @WrapMethod(method="deleteCursor",remap=false)
    private static boolean prominence$timeDelete(int x,int y,Operation<Boolean> original) {
        long start=System.nanoTime();try{return original.call(x,y);}finally{
            long elapsed=System.nanoTime()-start;
            if(elapsed>=50_000_000L)LoggerFactory.getLogger("Prominence compatibility").info("Slow EMI cursor deletion: elapsedMs={}",elapsed/1_000_000L);
        }
    }
}
