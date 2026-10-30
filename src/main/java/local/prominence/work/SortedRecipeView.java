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
    private final Map<EmiStack,List<EmiRecipe>> byInput=index(),byOutput=index();
    private final Map<EmiStack,List<EmiRecipe>> workstationAdds=new HashMap<>();
    public final int reusedRecipes,dynamicRecipes;

    public record Capture(Set<EmiRecipe> fixed,Map<EmiRecipe,RecipeStability.State> states,Map<Object,Comparison> comparisons){
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
        for(int i=0;i<recipes.size();i++){
            EmiRecipe r=recipes.get(i);
            if(base.getRecipes().get(i)!=r)return null;
            if(!RecipeStability.identityEquality(r)){
                org.slf4j.LoggerFactory.getLogger("Prominence compatibility").info("EMI sorted view fallback: custom recipe equality in {}",r.getClass().getName());return null;
            }
            var state=RecipeStability.capture(r);if(state!=null){fixed.add(r);states.put(r,state);}
        }
        // Custom stack/comparison functions must still see native second evaluation and copying.
        for(var map:List.of(access.prominence$inputs(),access.prominence$outputs()))for(var e:map.entrySet())
            if(!safeKey(e.getKey()))for(var r:e.getValue())fixed.remove(r);
        return new Capture(fixed,states,new HashMap<>(EmiComparisonDefaults.comparisons));
    }

    public static SortedRecipeView create(EmiRecipeManager base,List<EmiRecipeCategory> categories,
            Map<EmiRecipeCategory,List<EmiIngredient>> stations,List<EmiRecipe> recipes,Capture capture) {
        if(capture==null)return null;var fixed=capture.fixed();
        if(!capture.unchanged())return null;
        if(fixed==null||fixed.isEmpty()||!(base instanceof RecipeIndexAccess))return null;
        return new SortedRecipeView(base,categories,stations,recipes,fixed);
    }
    private SortedRecipeView(EmiRecipeManager base,List<EmiRecipeCategory> categories,
            Map<EmiRecipeCategory,List<EmiIngredient>> stations,List<EmiRecipe> recipes,Set<EmiRecipe> fixed) {
        this.categories=categories.stream().distinct().toList();this.recipes=List.copyOf(recipes);
        this.workstations=new HashMap<>();for(var e:stations.entrySet())workstations.put(e.getKey(),e.getValue().stream().distinct().toList());
        // Preserve native metadata callback order and duplicate-id first-wins.
        for(var r:recipes){var id=r.getId();var category=r.getCategory();
            if(!categories.contains(category))dev.emi.emi.runtime.EmiReloadLog.warn("Recipe "+id+" loaded with unregistered category: "+category.getId());
            byCategory.computeIfAbsent(category,k->new ArrayList<>()).add(r);if(id!=null)byId.putIfAbsent(id,r);}
        var rank=new IdentityHashMap<EmiRecipe,Integer>();int next=0;
        for(var category:byCategory.keySet()) {
            var list=byCategory.get(category);var comparator=EmiRecipeCategoryProperties.getSort(category);
            if(comparator!=EmiRecipeSorting.none()){list=list.stream().sorted(comparator).toList();EmiRecipeSorter.clear();}
            byCategory.put(category,list);for(var recipe:list)if(!rank.containsKey(recipe))rank.put(recipe,next++);
        }
        Map<EmiStack,Set<EmiRecipe>> inputs=index(),outputs=index();var access=(RecipeIndexAccess)base;
        seed(inputs,access.prominence$inputs(),fixed);seed(outputs,access.prominence$outputs(),fixed);
        int dynamic=0;
        for(var list:byCategory.values())for(var recipe:list)if(!fixed.contains(recipe)){
            dynamic++;
            for(var ingredient:recipe.getInputs())for(var s:ingredient.getEmiStacks())add(inputs,recipe,s);
            for(var ingredient:recipe.getCatalysts())for(var s:ingredient.getEmiStacks())add(inputs,recipe,s);
            for(var s:recipe.getOutputs())add(outputs,recipe,s);
        }
        finish(byInput,inputs,rank);finish(byOutput,outputs,rank);
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
    private static void seed(Map<EmiStack,Set<EmiRecipe>> target,Map<EmiStack,List<EmiRecipe>> source,Set<EmiRecipe> fixed){
        for(var e:source.entrySet()){
            Set<EmiRecipe> selected=null;
            for(var r:e.getValue())if(fixed.contains(r)){if(selected==null)selected=new LinkedHashSet<>();selected.add(r);}
            if(selected!=null)target.put(e.getKey().copy(),selected);
        }
    }
    private static void add(Map<EmiStack,Set<EmiRecipe>> target,EmiRecipe r,EmiStack s){
        if(!RecipeIndexInsertion.addExisting(target,r,s))target.computeIfAbsent(s.copy(),k->new LinkedHashSet<>()).add(r);
    }
    private static void finish(Map<EmiStack,List<EmiRecipe>> target,Map<EmiStack,Set<EmiRecipe>> source,IdentityHashMap<EmiRecipe,Integer> rank){
        var order=Comparator.comparingInt((EmiRecipe r)->rank.get(r));
        for(var e:source.entrySet())target.put(e.getKey(),e.getValue().stream().sorted(order).toList());
    }
    /** Called only after the owning worker/generation check, under the native worker monitor. */
    public void appendWorkstations(Map<EmiStack,List<EmiRecipe>> current){
        for(var e:workstationAdds.entrySet())current.computeIfAbsent(e.getKey(),k->new ArrayList<>()).addAll(e.getValue());
    }
    public List<EmiRecipeCategory> getCategories(){return categories;}
    public List<EmiIngredient> getWorkstations(EmiRecipeCategory c){return workstations.getOrDefault(c,List.of());}
    public List<EmiRecipe> getRecipes(){return recipes;}
    public List<EmiRecipe> getRecipes(EmiRecipeCategory c){return byCategory.getOrDefault(c,List.of());}
    public EmiRecipe getRecipe(class_2960 id){return byId.get(id);}
    public List<EmiRecipe> getRecipesByInput(EmiStack s){return byInput.getOrDefault(s,List.of());}
    public List<EmiRecipe> getRecipesByOutput(EmiStack s){return byOutput.getOrDefault(s,List.of());}
}
