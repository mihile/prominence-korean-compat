package fixture;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.stack.*;
import dev.emi.emi.registry.*;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;
import java.util.*;
import java.lang.reflect.*;
import net.minecraft.*;
public class RecipeFixture {
 public static int copies,checks;
 static void check(boolean ok){if(!ok)throw new AssertionError("check "+checks);checks++;}
 static EmiRecipe recipe(String id){return (EmiRecipe)Proxy.newProxyInstance(RecipeFixture.class.getClassLoader(),new Class[]{EmiRecipe.class},(p,m,a)->switch(m.getName()){case "toString"->id;case "hashCode"->System.identityHashCode(p);case "equals"->p==a[0];default->null;});}
 static class Custom extends ItemEmiStack {Custom(class_1792 i){super(i,null,1);} public EmiStack copy(){throw new IllegalStateException("custom copy preserved");}}
 public static List<String> run(boolean fixed)throws Exception {
  var manager=Class.forName("dev.emi.emi.registry.EmiRecipes$Manager");
  var count=ItemEmiStack.class.getField("fixtureCopies");count.setInt(null,0);
  List<String> output=new ArrayList<>();var a=new class_1792("stone");var b=new class_1792("tool");var c=new class_1792("custom");
  EmiComparisonDefaults.comparisons=new HashMap<>();EmiComparisonDefaults.comparisons.put(b,Comparison.compareNbt());
  EmiComparisonDefaults.comparisons.put(c,Comparison.of((x,y)->x.getAmount()==y.getAmount(),s->Long.hashCode(s.getAmount())));
  var r1=recipe("a");var r2=recipe("b");
  for(String name:List.of("lambda$new$3","lambda$new$6","lambda$new$8")) {
   var method=manager.getDeclaredMethod(name,Map.class,EmiRecipe.class,EmiStack.class);method.setAccessible(true);
   var map=new Object2ObjectOpenCustomHashMap<EmiStack,Set<EmiRecipe>>(new EmiStackList.ComparisonHashStrategy());
   for(int i=0;i<10000;i++){var s=new ItemEmiStack(a,null,1+i%64);method.invoke(null,map,i%2==0?r1:r2,s);}
   check(map.size()==1);var source=new ItemEmiStack(b,new class_2487("first"),1);method.invoke(null,map,r1,source);method.invoke(null,map,r2,source);
   var owned=map.keySet().stream().filter(s->s.getKey()==b).findFirst().orElseThrow();check(owned!=source);check(owned.getNbt()!=source.getNbt());
   source.getNbt().text="second";method.invoke(null,map,r2,source);check(map.size()==3);
   var rem=new ItemEmiStack(a,null,1).setRemainder(new ItemEmiStack(b,null,1));int before=count.getInt(null);method.invoke(null,map,r1,rem);check(count.getInt(null)>before);
   var custom=new ItemEmiStack(c,null,1);method.invoke(null,map,r1,custom);before=count.getInt(null);method.invoke(null,map,r2,custom);check(count.getInt(null)==before+1);
   try{method.invoke(null,map,r1,new Custom(a));throw new AssertionError();}catch(InvocationTargetException e){check(e.getCause() instanceof IllegalStateException);}
   for(var entry:map.object2ObjectEntrySet())output.add(name+":"+((class_1792)entry.getKey().getKey()).name+":"+(entry.getKey().getNbt()==null?"null":entry.getKey().getNbt().text)+":"+entry.getValue());
   var plain=new HashMap<EmiStack,Set<EmiRecipe>>();method.invoke(null,plain,r1,custom);before=count.getInt(null);method.invoke(null,plain,r2,custom);check(count.getInt(null)==before+1);
  }
  copies=count.getInt(null);Collections.sort(output);return output;
 }
}