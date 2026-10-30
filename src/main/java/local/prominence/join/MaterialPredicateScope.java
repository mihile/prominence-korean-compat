package local.prominence.join;

import com.google.common.collect.ImmutableList;
import java.util.*;
import net.minecraft.*;
import org.slf4j.LoggerFactory;

/** Filter only selective, pure native conditions before Iris' unchanged comparison loop. */
public final class MaterialPredicateScope implements AutoCloseable {
 private static final ThreadLocal<MaterialPredicateScope> CURRENT=new ThreadLocal<>();
 private record Rule(class_2769 property,Comparable value,Class<?> type){}
 private final MaterialPredicateScope previous;private final long start=System.nanoTime();
 private final Set<ImmutableList<class_2680>> assigned=Collections.newSetFromMap(new IdentityHashMap<>());
 private long patterns,input,retained,reused,skipped,fallbacks;private boolean closed;
 private MaterialPredicateScope(){previous=CURRENT.get();CURRENT.set(this);}
 public static MaterialPredicateScope begin(){return new MaterialPredicateScope();}
 public static ImmutableList<class_2680> unconditional(ImmutableList<class_2680> states){
  var scope=CURRENT.get();if(scope==null||states.isEmpty())return states;
  if(scope.assigned.contains(states)){scope.reused++;scope.skipped+=states.size();return ImmutableList.of();}
  for(var state:states)if(state.getClass()!=class_2680.class)return states;
  scope.assigned.add(states);return states;
 }
 private static boolean supported(class_2769<?> p){Class<?> c=p.getClass();if(c==class_2746.class||c==class_2758.class)return true;return (c==class_2754.class||c==class_2753.class)&&p.method_11902().getName().startsWith("net.minecraft.");}
 @SuppressWarnings({"rawtypes","unchecked"})
 public static ImmutableList<class_2680> filter(ImmutableList<class_2680> states,Map<class_2769<?>,String> conditions){
  var scope=CURRENT.get();if(scope==null||conditions==null||conditions.getClass()!=LinkedHashMap.class||conditions.isEmpty())return states;
  try{
   Rule best=null;int cardinality=3;
   for(var e:conditions.entrySet()){
    class_2769 p=e.getKey();String wanted=e.getValue();if(p==null||wanted==null||!supported(p))return states;
    if(p.getClass()!=class_2758.class)continue;
    int size=p.method_11898().size();if(size<=cardinality)continue;
    var parsed=p.method_11900(wanted);if(parsed.isEmpty())return states;Comparable value=(Comparable)parsed.get();if(!wanted.equals(p.method_11901(value)))return states;
    best=new Rule(p,value,p.method_11902());cardinality=size;
   }
   if(scope.assigned.contains(states)){scope.reused++;scope.skipped+=states.size();return ImmutableList.of();}
   if(best==null||states.size()<16)return states;
   var selected=ImmutableList.<class_2680>builder();int count=0;
   for(var state:states){
    if(state.getClass()!=class_2680.class)return states;
    Comparable value=state.method_11654(best.property());if(!best.type().isInstance(value))return states;
    if(value==best.value()||best.value().equals(value)){selected.add(state);count++;}
   }
   scope.patterns++;scope.input+=states.size();scope.retained+=count;
   return count==states.size()?states:selected.build();
  }catch(RuntimeException failure){scope.fallbacks++;return states;}
 }
 public void close(){if(closed)return;if(CURRENT.get()!=this)throw new IllegalStateException("Material scope closed out of order");closed=true;assigned.clear();if(previous==null)CURRENT.remove();else CURRENT.set(previous);if(previous==null)LoggerFactory.getLogger("Prominence compatibility").info("Iris material filtering: selectivePatterns={}, inputStates={}, retainedStates={}, reusedLists={}, skippedStates={}, nativeFallback={}, totalMs={}",patterns,input,retained,reused,skipped,fallbacks,(System.nanoTime()-start)/1e6);}
}
