package local.prominence.work;

import com.google.gson.JsonParser;
import dev.emi.emi.api.recipe.EmiRecipe;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.lang.reflect.Field;
import dev.emi.emi.api.stack.EmiStack;
import java.util.Set;
import net.minecraft.class_1792;

/** Only exact, fingerprinted native implementations with fixed list getters are admitted. */
public final class RecipeStability {
    private static final Map<String,List<String>> FIXED = load();
    private static Map<String,List<String>> load() {
        try(var in=RecipeStability.class.getResourceAsStream("/prominence-emi-fixed-recipes.json")) {
            if(in==null)return Map.of();
            var names=new HashMap<String,List<String>>();
            for(var entry:JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonObject().entrySet()) {
                var fields=new ArrayList<String>();for(var value:entry.getValue().getAsJsonArray())fields.add(value.getAsString());
                names.put(entry.getKey(),List.copyOf(fields));
            }
            return Map.copyOf(names);
        }catch(Exception|LinkageError e){return Map.of();}
    }
    private static final ClassValue<List<Field>> FIELDS=new ClassValue<>() {
        protected List<Field> computeValue(Class<?> type){
            var paths=FIXED.get(type.getName());if(paths==null)return List.of();
            try{var fields=new ArrayList<Field>();for(String path:paths){int split=path.indexOf('#');
                var owner=Class.forName(path.substring(0,split),false,type.getClassLoader());
                var field=owner.getDeclaredField(path.substring(split+1));field.setAccessible(true);if(!fields.contains(field))fields.add(field);
            }return List.copyOf(fields);}catch(Exception|LinkageError e){return List.of();}
        }
    };
    private static final ClassValue<Boolean> IDENTITY_EQUALITY=new ClassValue<>() {
        protected Boolean computeValue(Class<?> type){try{return type.getMethod("equals",Object.class).getDeclaringClass()==Object.class
                && type.getMethod("hashCode").getDeclaringClass()==Object.class;}catch(Exception|LinkageError e){return false;}}
    };
    public static boolean identityEquality(EmiRecipe r){return r!=null&&IDENTITY_EQUALITY.get(r.getClass());}
    public record State(Object recipe,List<Field> fields,List<Object> values,List<Object[]> elements,List<Integer> hashes) {
        public boolean unchanged(){try{
            for(int i=0;i<fields.size();i++){
                Object now=fields.get(i).get(recipe);if(now!=values.get(i))return false;
                if(now instanceof List<?> list){Object[] before=elements.get(i);if(list.size()!=before.length)return false;
                    for(int j=0;j<before.length;j++)if(list.get(j)!=before[j])return false;}
                if(contentHash(now)!=hashes.get(i))return false;
            }return true;
        }catch(Exception|LinkageError e){return false;}}
    }
    private static final Set<String> INGREDIENTS=Set.of("dev.emi.emi.api.stack.ItemEmiStack","dev.emi.emi.api.stack.FluidEmiStack",
        "dev.emi.emi.api.stack.TagEmiIngredient","dev.emi.emi.api.stack.ListEmiIngredient","dev.emi.emi.api.stack.EmptyEmiStack");
    private static boolean ingredient(Object x){return x!=null&&INGREDIENTS.contains(x.getClass().getName());}
    private static int contentHash(Object value){
        if(value instanceof List<?> list){int h=1;for(Object x:list)h=31*h+contentHash(x);return h;}
        if(value instanceof EmiStack s){int h=Objects.hashCode(s.getKey());h=31*h+Long.hashCode(s.getAmount());h=31*h+Objects.hashCode(s.getNbt());return 31*h+Objects.hashCode(s.getRemainder());}
        return System.identityHashCode(value);
    }
    public static State capture(EmiRecipe r) {
        Class<?> type=r.getClass();
        var fields=FIELDS.get(type);
        if(FIXED.containsKey(type.getName())&&!fields.isEmpty())try {
            var fs=new ArrayList<Field>();var values=new ArrayList<Object>();var elements=new ArrayList<Object[]>();var hashes=new ArrayList<Integer>();
            for(var f:fields){
                Object value=f.get(r);Object[] array=null;
                if(value instanceof List<?> list){
                    if(list.getClass().getClassLoader()!=null&&!list.getClass().getName().startsWith("com.google.common.collect."))return null;
                    array=list.toArray();for(Object x:array)if(!ingredient(x))return null;
                }else if(!ingredient(value))return null;
                fs.add(f);values.add(value);elements.add(array);hashes.add(contentHash(value));
            }return new State(r,fs,values,elements,hashes);
        }catch(Exception|LinkageError e){return null;}
        if(type.getName().equals("dev.emi.emi.recipe.special.EmiAnvilEnchantRecipe"))try{
            var f=type.getDeclaredField("tool");f.setAccessible(true);
            return EmiStackBuildScope.wasPlain((class_1792)f.get(r))?new State(r,List.of(),List.of(),List.of(),List.of()):null;
        }catch(Exception|LinkageError e){return null;}
        return null;
    }
    private RecipeStability(){}
}
