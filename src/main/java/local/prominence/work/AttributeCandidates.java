package local.prominence.work;

import elocindev.customitemattributes.api.ItemProperty;
import java.util.*;
import java.util.function.Supplier;
import net.minecraft.class_2960;
import org.slf4j.LoggerFactory;

/** Search-build-only candidate index. Never cache attribute values, stacks, slots or modifier results. */
public final class AttributeCandidates implements AutoCloseable {
    private static final ThreadLocal<AttributeCandidates> CURRENT=new ThreadLocal<>();
    private final AttributeCandidates previous;
    private Index index;
    private long calls,rebuilds,originalEntries,selectedEntries,fallbacks;
    private boolean closed;
    private AttributeCandidates(){previous=CURRENT.get();CURRENT.set(this);}
    public static AttributeCandidates begin(){return new AttributeCandidates();}
    public static boolean active(){return CURRENT.get()!=null;}

    /** null requests the untouched original iterator, including its error ordering. */
    public static List<ItemProperty> select(List<ItemProperty> source,class_2960 target){
        return select(source,()->target);
    }
    public static List<ItemProperty> select(List<ItemProperty> source,Supplier<class_2960> target){
        var scope=CURRENT.get();
        if(scope==null||source==null||target==null||source.getClass()!=ArrayList.class)return null;
        scope.calls++;
        try {
            if(scope.index==null||!scope.index.matches(source)){scope.index=new Index(source);scope.rebuilds++;}
            if(scope.index.byId==null){scope.fallbacks++;return null;}
        }catch(RuntimeException error){scope.index=null;scope.fallbacks++;return null;}
        // Validate all definitions before consulting the input. Malformed/native
        // fallback must not move stack getter errors ahead of property errors.
        class_2960 id=target.get();
        if(id==null){scope.fallbacks++;return null;}
        var result=scope.index.byId.getOrDefault(id,List.of());scope.originalEntries+=source.size();scope.selectedEntries+=result.size();return result;
    }

    private static final class Index {
        final List<ItemProperty> source;
        final Object[] properties;
        final String[] ids;
        Map<class_2960,List<ItemProperty>> byId;
        Index(List<ItemProperty> source){
            this.source=source;properties=source.toArray();ids=new String[properties.length];
            for(int i=0;i<properties.length;i++){
                if(!(properties[i] instanceof ItemProperty property)||property.getClass()!=ItemProperty.class)return;
                ids[i]=property.item;
            }
            var entries=new HashMap<class_2960,List<ItemProperty>>();
            for(int i=0;i<properties.length;i++){
                // Parsing is pure. A malformed/null ID falls back to the native full loop;
                // it must not throw here before earlier native mutations have occurred.
                var id=new class_2960(ids[i]);entries.computeIfAbsent(id,key->new ArrayList<>()).add((ItemProperty)properties[i]);
            }
            entries.replaceAll((key,value)->List.copyOf(value));byId=entries;
        }
        boolean matches(List<ItemProperty> candidate){
            if(candidate!=source||candidate.size()!=properties.length)return false;
            for(int i=0;i<properties.length;i++){
                Object value=candidate.get(i);
                if(value!=properties[i]||!(value instanceof ItemProperty property)||property.item!=ids[i])return false;
            }
            return true;
        }
    }
    public long originalEntries(){return originalEntries;}
    public long selectedEntries(){return selectedEntries;}
    public long rebuilds(){return rebuilds;}
    public void close(){
        if(closed)return;
        if(CURRENT.get()!=this)throw new IllegalStateException("Attribute scope closed out of order");
        closed=true;index=null;if(previous==null)CURRENT.remove();else CURRENT.set(previous);
        LoggerFactory.getLogger("Prominence compatibility").info("EMI attribute candidates: calls={}, rebuilds={}, originalEntries={}, selectedEntries={}, fallbacks={}",calls,rebuilds,originalEntries,selectedEntries,fallbacks);
    }
}
