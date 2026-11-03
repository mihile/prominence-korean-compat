package local.prominence.memory;

import java.lang.reflect.*;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.function.Predicate;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.*;
import org.slf4j.LoggerFactory;

/** Main-thread lifecycle cleanup. No world/recipe mutation, forced GC or recurring heap scans. */
public final class RetentionCleanupClient implements ClientModInitializer {
    private static final Map<RetainedClientOwner,Boolean> AZURE=Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<RetainedClientOwner,Boolean> SCREENS=Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<RetainedClientOwner,Boolean> ARMOR=Collections.synchronizedMap(new WeakHashMap<>());
    private final Map<String,Boolean> enabled=new LinkedHashMap<>();
    private final WorldReferencePruner changes=new WorldReferencePruner();
    private WeakReference<Object> connection=new WeakReference<>(null);
    private Field handlers,listener,packetConnection,aePlayers,legendary,repair,repairPlayer,inventoryPlayer,stackKeys;
    private Class<?> clientPacket;
    private ArmorQueryPruner armor;
    private int ticks, retries;
    public static void trackAzure(RetainedClientOwner owner){AZURE.put(owner,Boolean.TRUE);}
    public static void trackScreen(RetainedClientOwner owner){SCREENS.put(owner,Boolean.TRUE);}
    public static void trackArmor(RetainedClientOwner owner){ARMOR.put(owner,Boolean.TRUE);}
    public void onInitializeClient() {
        for(String key:List.of("BadPackets","Ae2","Legendary","Zenith","Azure","Screens","Armor","StackKeys","ArmorRenderers"))
            enabled.put(key,WorldCacheGuard.installed("retention"+key));
        LoggerFactory.getLogger("Prominence compatibility").info("Client retained-owner cleanup: {}",enabled);
        if(enabled.containsValue(true))ClientTickEvents.END_CLIENT_TICK.register(this::afterTick);
    }
    static Class<?> type(String name)throws ClassNotFoundException{return Class.forName(name,false,RetentionCleanupClient.class.getClassLoader());}
    static Field field(Class<?> owner,String name,boolean isStatic,Class<?> expected,boolean writable)throws ReflectiveOperationException {
        Field result=owner.getDeclaredField(name);
        if(Modifier.isStatic(result.getModifiers())!=isStatic||!expected.isAssignableFrom(result.getType())
            ||writable&&Modifier.isFinal(result.getModifiers()))throw new IllegalStateException("Unknown retained-owner shape: "+owner.getName()+"."+name);
        result.setAccessible(true);return result;
    }
    @SuppressWarnings("unchecked") private static Map<Object,Object> map(Field field)throws IllegalAccessException {
        Object value=field.get(null);if(!(value instanceof Map<?,?>))throw new IllegalStateException("Unknown map cache");return (Map<Object,Object>)value;
    }
    static boolean oldOwner(Object value,class_638 world,class_746 player) {
        if(value instanceof class_638 w)return w!=world;
        if(value instanceof class_746 p)return p!=player||p.method_37908()!=world;
        if(value instanceof class_1297 entity)return entity.method_37908() instanceof class_638 w&&w!=world;
        return false;
    }
    private int badPackets(class_634 current)throws ReflectiveOperationException {
        Class<?> registry=type("lol.bai.badpackets.impl.registry.ChannelRegistry");
        if(handlers==null) {
            clientPacket=type("lol.bai.badpackets.impl.handler.ClientPacketHandler");
            Field list=field(registry,"handlers",false,Set.class,false);
            Field owner=field(clientPacket,"listener",false,class_634.class,false);
            Field net=field(type("lol.bai.badpackets.impl.handler.AbstractPacketHandler"),"connection",false,class_2535.class,false);
            listener=owner;packetConnection=net;handlers=list;
        }
        Object store=field(registry,"S2C",true,registry,false).get(null);
        @SuppressWarnings("unchecked") Set<Object> set=(Set<Object>)handlers.get(store);int released=0;
        for(Object handler:new ArrayList<>(set)) {
            if(!clientPacket.isInstance(handler)||listener.get(handler)==current)continue;
            class_2535 net=(class_2535)packetConnection.get(handler);
            // Preserve active or not-yet-published connections during login.
            if(net!=null&&!net.method_10758()&&set.remove(handler))released++;
        }
        return released;
    }
    private int ae2(class_638 world,class_746 player)throws ReflectiveOperationException {
        if(aePlayers==null)aePlayers=field(type("de.mari_023.ae2wtlib.wct.CraftingTerminalHandler"),"players",true,WeakHashMap.class,false);
        return WorldCacheReleaser.remove(map(aePlayers),e->e.getKey() instanceof class_1657&&oldOwner(e.getKey(),world,player));
    }
    private int legendary(boolean transition)throws ReflectiveOperationException {
        if(legendary==null)legendary=field(type("com.anthonyhilyard.legendarytooltips.config.LegendaryTooltipsConfig"),"frameDefinitionCache",true,Map.class,false);
        var cache=map(legendary);int size=cache.size();
        // Entirely recomputable frame lookups. Keep native config/color sources.
        if(transition||size>4096){cache.clear();return size;}return 0;
    }
    private int zenith(class_638 world,class_746 player)throws ReflectiveOperationException {
        if(repair==null) {
            Class<?> event=type("dev.shadowsoffire.apotheosis.util.Events$RepairEvent");
            Field value=field(class_1706.class,"zenith$event",true,event,true);
            repairPlayer=field(event,"player",false,class_1657.class,false);repair=value;
        }
        Object value=repair.get(null);
        if(value==null||!oldOwner(repairPlayer.get(value),world,player))return 0;
        // Static scratch is shared with a local server; caller excludes that case.
        Field[] stacks=new Field[3];String[] names={"zenith$leftItem","zenith$rightItem","zenith$output"};
        for(int i=0;i<3;i++)stacks[i]=field(class_1706.class,names[i],true,class_1799.class,true);
        if(repair.get(null)!=value)return 0;
        repair.set(null,null);for(Field stack:stacks)stack.set(null,class_1799.field_8037);return 1;
    }
    private boolean oldScreen(Object value,class_638 world,class_746 player) {
        if(!(value instanceof class_490 screen))return false;
        try {
            if(inventoryPlayer==null)inventoryPlayer=field(class_1723.class,"field_7828",false,class_1657.class,false);
            return oldOwner(inventoryPlayer.get(screen.method_17577()),world,player);
        }catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
    }
    static int releaseOwners(Map<RetainedClientOwner,Boolean> owners,Predicate<Object> obsolete) {
        int count=0;
        synchronized(owners) {
            for(var owner:owners.keySet()) {
                Object value=owner.prominence$retainedOwner();
                if(value!=null&&obsolete.test(value)&&owner.prominence$retainedOwner()==value){owner.prominence$releaseOwner();count++;}
            }
        }
        return count;
    }
    private int keys()throws ReflectiveOperationException {
        if(stackKeys==null)stackKeys=field(type("net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey"),"CACHE",true,Map.class,false);
        var cache=map(stackKeys);int size=cache.size();if(size>0)cache.clear();return size;
    }
    @FunctionalInterface private interface Cleanup {int run()throws ReflectiveOperationException;}
    private void run(String key,Cleanup cleanup,boolean report) {
        if(!Boolean.TRUE.equals(enabled.get(key)))return;
        try {
            int count=cleanup.run();
            if(report&&count>0)LoggerFactory.getLogger("Prominence compatibility").info("Client obsolete owners released: module={}, entries={}",key,count);
        }catch(ReflectiveOperationException|RuntimeException|LinkageError error) {
            enabled.put(key,false);LoggerFactory.getLogger("Prominence compatibility").warn("Retained-owner cleanup disabled for {}; preserving native data",key,error);
        }
    }
    private void afterTick(class_310 client) {
        class_634 current=client.method_1562();boolean transition=changes.transition(client.field_1687,client.field_1724)||connection.get()!=current;
        if(transition)connection=new WeakReference<>(current);
        // Match the native cache's tick lifetime; multiplayer clients otherwise
        // never execute CommonEventHandler's server-tick invalidation.
        if(client.method_1576()==null)run("StackKeys",this::keys,false);
        ticks++;
        if(!transition&&ticks%20!=0)return;
        run("Legendary",()->legendary(transition),transition);
        // Socket closure and deferred renderer callbacks can finish just after
        // the transition tick. Retry small registries twice, not a heap scan.
        if(transition)retries=2;
        if(!transition&&retries==0){run("BadPackets",()->badPackets(current),true);return;}
        if(!transition)retries--;
        run("BadPackets",()->badPackets(current),true);
        if(client.method_1576()==null) {
            run("Ae2",()->ae2(client.field_1687,client.field_1724),true);
            run("Zenith",()->zenith(client.field_1687,client.field_1724),true);
        }
        run("Azure",()->releaseOwners(AZURE,owner->oldOwner(owner,client.field_1687,client.field_1724)),true);
        run("ArmorRenderers",()->releaseOwners(ARMOR,owner->oldOwner(owner,client.field_1687,client.field_1724)),true);
        run("Screens",()->releaseOwners(SCREENS,owner->oldScreen(owner,client.field_1687,client.field_1724)),true);
        run("Armor",()->{if(armor==null)armor=new ArmorQueryPruner();return armor.release(owner->oldOwner(owner,client.field_1687,client.field_1724));},true);
    }
}
