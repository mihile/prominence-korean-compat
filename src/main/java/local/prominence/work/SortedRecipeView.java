package local.prominence.work;

import dev.emi.emi.api.recipe.*;
import dev.emi.emi.api.stack.*;
import dev.emi.emi.data.EmiRecipeCategoryProperties;
import dev.emi.emi.registry.EmiComparisonDefaults;
import dev.emi.emi.registry.EmiRecipeSorter;
import dev.emi.emi.registry.EmiStackList;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;
import java.util.*;
import net.minecraft.class_2960;

/** Reorder completed memberships; only dynamic recipes expand their ingredients a second time. */
public final class SortedRecipeView implements EmiRecipeManager {
    private final List<EmiRecipeCategory> categories;
    private final Map<EmiRecipeCategory,List<EmiIngredient>> workstations;
    private final List<EmiRecipe> recipes;
    private final Map<EmiRecipeCategory,List<EmiRecipe>> byCategory=new HashMap<>();
    private final Map<class_2960,EmiRecipe> byId=new HashMap<>();
    private final Map<EmiStack,Bucket> byInput=index(),byOutput=index();
    private final Map<EmiStack,List<EmiRecipe>> workstationAdds=new HashMap<>();
    public final int reusedRecipes,dynamicRecipes;

    public record Capture(Set<EmiRecipe> fixed,Map<EmiRecipe,RecipeStability.State> states,Map<Object,Comparison> comparisons,Set<EmiRecipe> repeated){
        public boolean unchanged(){
            if(EmiComparisonDefaults.comparisons.size()!=comparisons.size())return false;
            for(var e:comparisons.entrySet())if(EmiComparisonDefaults.comparisons.get(e.getKey())!=e.getValue())return false;
            for(var entry:states.entrySet())if(fixed.contains(entry.getKey())&&!entry.getValue().unchanged())return false;return true;
        }
    }
    public static Capture capture(EmiRecipeManager base,List<EmiRecipe> recipes) {
        if(!(base instanceof RecipeIndexAccess access)||base.getRecipes().size()!=recipes.size())return null;
        for(var map:List.of(access.prominence$inputs(),access.prominence$outputs()))
            if(map.getClass()!=Object2ObjectOpenCustomHashMap.class
                    ||((Object2ObjectOpenCustomHashMap<?,?>)map).strategy().getClass()!=EmiStackList.ComparisonHashStrategy.class)return null;
        Set<EmiRecipe> fixed=Collections.newSetFromMap(new IdentityHashMap<>());
        var states=new IdentityHashMap<EmiRecipe,RecipeStability.State>();
        Set<EmiRecipe> seen=Collections.newSetFromMap(new IdentityHashMap<>()),repeated=Collections.newSetFromMap(new IdentityHashMap<>());
        for(int i=0;i<recipes.size();i++){
            EmiRecipe r=recipes.get(i);if(!seen.add(r))repeated.add(r);
            if(base.getRecipes().get(i)!=r)return null;
            if(!RecipeStability.identityEquality(r)){
                org.slf4j.LoggerFactory.getLogger("Prominence compatibility").info("EMI sorted view fallback: custom recipe equality in {}",r.getClass().getName());return null;
            }
            var state=RecipeStability.capture(r);if(state!=null){fixed.add(r);states.put(r,state);}
        }
        // Custom stack/comparison functions must still see native second evaluation and copying.
        for(var map:List.of(access.prominence$inputs(),access.prominence$outputs()))for(var e:map.entrySet())
            if(!safeKey(e.getKey()))for(var r:e.getValue())fixed.remove(r);
        return new Capture(fixed,states,new HashMap<>(EmiComparisonDefaults.comparisons),repeated);
    }

    public static SortedRecipeView create(EmiRecipeManager base,List<EmiRecipeCategory> categories,
            Map<EmiRecipeCategory,List<EmiIngredient>> stations,List<EmiRecipe> recipes,Capture capture) {
        if(capture==null)return null;var fixed=capture.fixed();
        if(!capture.unchanged())return null;
        if(fixed==null||fixed.isEmpty()||!(base instanceof RecipeIndexAccess))return null;
        return new SortedRecipeView(base,categories,stations,recipes,fixed,capture.repeated());
    }
    private SortedRecipeView(EmiRecipeManager base,List<EmiRecipeCategory> categories,
            Map<EmiRecipeCategory,List<EmiIngredient>> stations,List<EmiRecipe> recipes,Set<EmiRecipe> fixed,Set<EmiRecipe> repeated) {
        this.categories=categories.stream().distinct().toList();this.recipes=List.copyOf(recipes);
        this.workstations=new HashMap<>();for(var e:stations.entrySet())workstations.put(e.getKey(),e.getValue().stream().distinct().toList());
        // Preserve native metadata callback order and duplicate-id first-wins.
        for(var r:recipes){var id=r.getId();var category=r.getCategory();
            if(!categories.contains(category))dev.emi.emi.runtime.EmiReloadLog.warn("Recipe "+id+" loaded with unregistered category: "+category.getId());
            byCategory.computeIfAbsent(category,k->new ArrayList<>()).add(r);if(id!=null)byId.putIfAbsent(id,r);}
        for(var category:byCategory.keySet()) {
            var list=byCategory.get(category);var comparator=EmiRecipeCategoryProperties.getSort(category);
            if(comparator!=EmiRecipeSorting.none()){list=list.stream().sorted(comparator).toList();EmiRecipeSorter.clear();}
            byCategory.put(category,list);
        }
        var access=(RecipeIndexAccess)base;
        var inputLinks=seed(byInput,access.prominence$inputs(),fixed);
        var outputLinks=seed(byOutput,access.prominence$outputs(),fixed);
        int dynamic=0;
        for(var list:byCategory.values())for(var recipe:list){
            if(fixed.contains(recipe)){
                replay(inputLinks.get(recipe),recipe,repeated);replay(outputLinks.get(recipe),recipe,repeated);
            }else{
                dynamic++;
                for(var ingredient:recipe.getInputs())for(var stack:ingredient.getEmiStacks())add(byInput,recipe,stack,repeated);
                for(var ingredient:recipe.getCatalysts())for(var stack:ingredient.getEmiStacks())add(byInput,recipe,stack,repeated);
                for(var stack:recipe.getOutputs())add(byOutput,recipe,stack,repeated);
            }
        }
        freeze(byInput);freeze(byOutput);
        for(var e:byCategory.entrySet())for(var ingredient:workstations.getOrDefault(e.getKey(),List.of()))
            for(var stack:ingredient.getEmiStacks())workstationAdds.computeIfAbsent(stack,k->new ArrayList<>()).addAll(e.getValue());
        reusedRecipes=fixed.size();dynamicRecipes=dynamic;
    }
    private static boolean safeKey(EmiStack s) {
        if(s==null||(s.getClass()!=ItemEmiStack.class&&s.getClass()!=FluidEmiStack.class))return false;
        var cmp=EmiComparisonDefaults.get(s.getKey());
        return (cmp==Comparison.DEFAULT_COMPARISON||cmp==Comparison.compareNbt())&&s.getRemainder()==EmiStack.EMPTY;
    }
    private static <V> Map<EmiStack,V> index(){return new Object2ObjectOpenCustomHashMap<>(new EmiStackList.ComparisonHashStrategy());}
    private static final class Bucket {
        private List<EmiRecipe> recipes=new ArrayList<>();
        private EmiRecipe last;
        private Set<EmiRecipe> repeatedSeen;
        void add(EmiRecipe recipe,Set<EmiRecipe> repeated){
            if(last==recipe)return;
            last=recipe;
            if(!repeated.isEmpty()&&repeated.contains(recipe)){
                if(repeatedSeen==null)repeatedSeen=Collections.newSetFromMap(new IdentityHashMap<>());
                if(!repeatedSeen.add(recipe))return;
            }
            recipes.add(recipe);
        }
        void freeze(){recipes=List.copyOf(recipes);last=null;repeatedSeen=null;}
    }
    private static IdentityHashMap<EmiRecipe,List<Bucket>> seed(Map<EmiStack,Bucket> target,
            Map<EmiStack,List<EmiRecipe>> source,Set<EmiRecipe> fixed){
        var links=new IdentityHashMap<EmiRecipe,List<Bucket>>();
        for(var entry:source.entrySet()){
            Bucket bucket=null;
            for(var recipe:entry.getValue())if(fixed.contains(recipe)){
                if(bucket==null){bucket=new Bucket();target.put(entry.getKey().copy(),bucket);}
                links.computeIfAbsent(recipe,k->new ArrayList<>(2)).add(bucket);
            }
        }
        return links;
    }
    private static void replay(List<Bucket> buckets,EmiRecipe recipe,Set<EmiRecipe> repeated){
        if(buckets!=null)for(var bucket:buckets)bucket.add(recipe,repeated);
    }
    private static void add(Map<EmiStack,Bucket> target,EmiRecipe recipe,EmiStack stack,Set<EmiRecipe> repeated){
        Bucket bucket=null;
        // Same safe no-copy hit as native RecipeIndexInsertion; other stack types retain native copy-before-lookup.
        if(stack.getClass()==ItemEmiStack.class&&safeKey(stack))bucket=target.get(stack);
        if(bucket==null)bucket=target.computeIfAbsent(stack.copy(),k->new Bucket());
        bucket.add(recipe,repeated);
    }
    private static void freeze(Map<EmiStack,Bucket> buckets){for(var bucket:buckets.values())bucket.freeze();}
    /** Called only after the owning worker/generation check, under the native worker monitor. */
    public void appendWorkstations(Map<EmiStack,List<EmiRecipe>> current){
        for(var e:workstationAdds.entrySet())current.computeIfAbsent(e.getKey(),k->new ArrayList<>()).addAll(e.getValue());
    }
    public List<EmiRecipeCategory> getCategories(){return categories;}
    public List<EmiIngredient> getWorkstations(EmiRecipeCategory c){return workstations.getOrDefault(c,List.of());}
    public List<EmiRecipe> getRecipes(){return recipes;}
    public List<EmiRecipe> getRecipes(EmiRecipeCategory c){return byCategory.getOrDefault(c,List.of());}
    public EmiRecipe getRecipe(class_2960 id){return byId.get(id);}
    public List<EmiRecipe> getRecipesByInput(EmiStack s){var bucket=byInput.get(s);return bucket==null?List.of():bucket.recipes;}
    public List<EmiRecipe> getRecipesByOutput(EmiStack s){var bucket=byOutput.get(s);return bucket==null?List.of():bucket.recipes;}
}
