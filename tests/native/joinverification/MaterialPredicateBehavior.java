package local.prominence.join;
import net.minecraft.*;import java.util.*;import com.google.common.collect.ImmutableList;
public final class MaterialPredicateBehavior {
 static int checks;static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
 static List<class_2680> nativeResult(ImmutableList<class_2680> states,Map<class_2769<?>,String> p){var out=new ArrayList<class_2680>();for(var s:states)if(NativeMaterialPredicate.check(s,p))out.add(s);return out;}
 static void same(ImmutableList<class_2680> states,Map<class_2769<?>,String> p,String why){check(nativeResult(states,p).equals(nativeResult(MaterialPredicateScope.filter(states,p),p)),why);}
 static class Custom extends class_2758 {}
 public static void main(String[] args)throws Exception{
  var age=new class_2758();var lit=new class_2746();var axis=new class_2754();var builder=ImmutableList.<class_2680>builder();for(int n=0;n<16;n++)for(boolean b:new boolean[]{false,true})for(var v:Axis.values())builder.add(new class_2680(Map.of(age,n,lit,b,axis,v)));var states=builder.build();
  var p=new LinkedHashMap<class_2769<?>,String>();p.put(age,"4");check(MaterialPredicateScope.filter(states,p)==states,"outside scope remains native");
  long before=class_2769.names;for(int n=0;n<1000;n++)nativeResult(states,p);long baseline=class_2769.names-before;before=class_2769.names;
  try(var scope=MaterialPredicateScope.begin()){
   for(int n=0;n<1000;n++)check(nativeResult(MaterialPredicateScope.filter(states,p),p).size()==2*Axis.values().length,"native final loop still runs on selected states");
   long optimized=class_2769.names-before;check(optimized<baseline/2,"reduce actual native value-name calls "+baseline+" -> "+optimized);
   for(int n=0;n<16;n++){p.put(age,Integer.toString(n));same(states,p,"all integer predicates");p.put(lit,"true");same(states,p,"mixed predicates");p.remove(age);p.put(age,Integer.toString(n));same(states,p,"ordering");p.remove(lit);}
   var bool=new LinkedHashMap<class_2769<?>,String>();bool.put(lit,"true");long names=class_2769.names;check(MaterialPredicateScope.filter(states,bool)==states&&class_2769.names==names,"simple boolean uses exactly the original list and comparison loop");
   var e=new LinkedHashMap<class_2769<?>,String>();e.put(axis,"y");same(states,e,"enum predicate");p.put(age,"04");same(states,p,"numeric alias is not exact name");p.put(age,"invalid");same(states,p,"invalid value stays native");
   var ids=new LinkedHashMap<class_2680,Integer>();var expectedIds=new LinkedHashMap<class_2680,Integer>();
   p.put(age,"4");for(var state:states)if(NativeMaterialPredicate.check(state,p)){expectedIds.putIfAbsent(state,1);}
   for(var state:MaterialPredicateScope.filter(states,p))if(NativeMaterialPredicate.check(state,p))ids.putIfAbsent(state,1);
   for(var state:states)expectedIds.putIfAbsent(state,2);for(var state:MaterialPredicateScope.unconditional(states))ids.putIfAbsent(state,2);
   check(MaterialPredicateScope.unconditional(states).isEmpty(),"already fully assigned unconditional states are skipped");
   check(MaterialPredicateScope.filter(states,p).isEmpty(),"already fully assigned conditional states are skipped");
   for(var state:states)if(NativeMaterialPredicate.check(state,p))expectedIds.putIfAbsent(state,3);
   check(ids.equals(expectedIds)&&new ArrayList<>(ids.keySet()).equals(new ArrayList<>(expectedIds.keySet())),"native first-wins material IDs and insertion order");
   try(var fresh=MaterialPredicateScope.begin()){p.put(age,"4");var mutable=new LinkedHashMap<>(p);mutable.entrySet().iterator().next().setValue("5");same(states,mutable,"entry mutation between builds");mutable.compute(age,(k,v)->"6");same(states,mutable,"compute mutation");
   check(MaterialPredicateScope.filter(states,Map.of(age,"4"))==states,"custom maps use native path");var custom=new Custom();var c=new LinkedHashMap<class_2769<?>,String>();c.put(custom,"1");check(MaterialPredicateScope.filter(states,c)==states,"custom property stays native");
   var missing=ImmutableList.of(new class_2680(Map.of(lit,false)));var order=new LinkedHashMap<class_2769<?>,String>();order.put(lit,"true");order.put(age,"3");same(missing,order,"native short circuit");order.remove(lit);order.put(lit,"true");try{nativeResult(MaterialPredicateScope.filter(missing,order),order);throw new AssertionError("native missing property should throw");}catch(IllegalArgumentException expected){checks++;}
   }
   try(var inner=MaterialPredicateScope.begin()){same(states,p,"nested build");}check(MaterialPredicateScope.filter(states,p).isEmpty(),"outer assigned-list scope restored");final boolean[] isolated={false};Thread t=new Thread(()->isolated[0]=MaterialPredicateScope.filter(states,p)==states);t.start();t.join();check(isolated[0],"scope stays thread local");
   System.out.println("NATIVE conversions "+baseline+" -> "+optimized+"; whole-game speed is unmeasured");
  }
  check(MaterialPredicateScope.filter(states,p)==states,"scope removed");try{try(var scope=MaterialPredicateScope.begin()){throw new IllegalStateException();}}catch(IllegalStateException expected){}check(MaterialPredicateScope.filter(states,p)==states,"exception scope removed");
  System.out.println("PASS "+checks+" native final-result/order/short-circuit/alias/mutation/custom/thread/exception checks; API doubles, no game");
 }
}
