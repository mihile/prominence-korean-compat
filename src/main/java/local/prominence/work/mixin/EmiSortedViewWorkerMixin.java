package local.prominence.work.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.emi.emi.api.recipe.*;
import dev.emi.emi.api.stack.*;
import dev.emi.emi.config.EmiConfig;
import dev.emi.emi.registry.EmiRecipes;
import java.util.*;
import local.prominence.work.*;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets="dev.emi.emi.registry.EmiRecipes$Worker",remap=false)
public abstract class EmiSortedViewWorkerMixin {
    @Shadow private List<EmiRecipeCategory> categories;
    @Shadow private Map<EmiRecipeCategory,List<EmiIngredient>> workstations;
    @Shadow private List<EmiRecipe> recipes;
    @Unique private EmiRecipeManager prominence$base;
    @Unique private SortedRecipeView.Capture prominence$fixed;
    @Inject(method="<init>",at=@At("RETURN"),remap=false)
    private void prominence$capture(CallbackInfo ci){
        if(!EmiConfig.devMode&&!EmiConfig.logNonTagIngredients){
            prominence$base=EmiRecipes.manager;
            prominence$fixed=SortedRecipeView.capture(prominence$base,recipes);
            LoggerFactory.getLogger("Prominence compatibility").info("EMI sorted view eligibility: fixedRecipes={}, totalRecipes={}",
                prominence$fixed==null?0:prominence$fixed.fixed().size(),recipes.size());
        }
    }
    @WrapMethod(method="run",remap=false)
    private void prominence$reuseCompletedMemberships(Operation<Void> original){
        try {
            if(prominence$fixed==null||prominence$fixed.fixed().isEmpty()){original.call();return;}
            synchronized(EmiRecipes.class){if((Object)EmiRecipes.activeWorker!=this||EmiRecipes.manager!=prominence$base)return;}
            long start=System.nanoTime();SortedRecipeView view;
            try(var scope=EmiStackBuildScope.begin("sorted-view")){
                view=SortedRecipeView.create(prominence$base,categories,workstations,recipes,prominence$fixed);
            }
            if(view==null||!prominence$fixed.unchanged()){
                LoggerFactory.getLogger("Prominence compatibility").info("EMI sorted view fallback: source fields or stack values changed");
                original.call();return;
            }
            synchronized(EmiRecipes.class){
                if((Object)EmiRecipes.activeWorker!=this||EmiRecipes.manager!=prominence$base)return;
                view.appendWorkstations(EmiRecipes.byWorkstation);
                EmiRecipes.manager=view;
                EmiRecipes.activeWorker=null;
            }
            LoggerFactory.getLogger("Prominence compatibility").info(
                "EMI sorted relationship view: reusedRecipes={}, dynamicRecipes={}, elapsedMs={}",
                view.reusedRecipes,view.dynamicRecipes,(System.nanoTime()-start)/1_000_000.0);
        }finally{prominence$base=null;prominence$fixed=null;}
    }
}
