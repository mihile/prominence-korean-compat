package local.prominence.memory;

import java.lang.reflect.*;
import java.util.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.class_1297;
import net.minecraft.class_1309;
import net.minecraft.class_1661;
import net.minecraft.class_2586;
import net.minecraft.class_310;
import net.minecraft.class_638;
import net.minecraft.class_746;
import org.slf4j.LoggerFactory;

/** Runs after actual world/player changes, when render/input/event stacks have returned. */
public final class WorldCacheCleanupClient implements ClientModInitializer {
    private final WorldReferencePruner changes=new WorldReferencePruner();
    private boolean iceberg,inventories,mi,balm;
    private Field wrappers,previews,wrappedWorld,inventoryWrappers,zenithOwner,keyMap,fallLocal;
    private Class<?> collectorType,zenithInventoryType,fallType;
    private Method fallEntity;
    private Field[][] previewPairs;
    public void onInitializeClient() {
        iceberg=WorldCacheGuard.installed("iceberg");inventories=WorldCacheGuard.installed("inventories");
        mi=WorldCacheGuard.installed("mi");balm=WorldCacheGuard.installed("balm");
        LoggerFactory.getLogger("Prominence compatibility").info("Client world cache lifetimes: iceberg={}, inventories={}, mi={}, balm={}",state(iceberg),state(inventories),state(mi),state(balm));
        if(iceberg||inventories||mi||balm)ClientTickEvents.END_CLIENT_TICK.register(this::afterTick);
    }
    private static String state(boolean enabled){return enabled?"enabled":"disabled";}
    private static Class<?> type(String name)throws ClassNotFoundException{return Class.forName(name,false,WorldCacheCleanupClient.class.getClassLoader());}
    private static Field field(Class<?> owner,String name,boolean isStatic,Class<?> expected,boolean writable)throws ReflectiveOperationException {
        Field f=owner.getDeclaredField(name);
        if(Modifier.isStatic(f.getModifiers())!=isStatic||!expected.isAssignableFrom(f.getType())||(writable&&Modifier.isFinal(f.getModifiers())))throw new IllegalStateException("Unexpected cache field "+owner.getName()+"."+name);
        f.setAccessible(true);return f;
    }
    @SuppressWarnings("unchecked") private static Map<Object,Object> map(Field f)throws IllegalAccessException {
        Object value=f.get(null);if(!(value instanceof Map<?,?>))throw new IllegalStateException("Unknown cache map shape");return (Map<Object,Object>)value;
    }
    private boolean oldWorld(Object world,Object current)throws IllegalAccessException {
        if(world instanceof class_638)return world!=current;
        if(collectorType!=null&&collectorType.isInstance(world)) {
            Object wrapped=wrappedWorld.get(world);return wrapped instanceof class_638&&wrapped!=current;
        }
        return false;
    }
    private boolean oldOwner(Object owner,Object current) {
        try {
            if(owner instanceof class_1297 entity)return oldWorld(entity.method_37908(),current);
            if(owner instanceof class_2586 block)return oldWorld(block.method_10997(),current);
            return false;
        }catch(IllegalAccessException e){throw new IllegalStateException(e);}
    }
    private int iceberg(Object current)throws ReflectiveOperationException {
        if(wrappers==null) {
            collectorType=type("com.anthonyhilyard.iceberg.util.EntityCollector");
            wrappedWorld=field(collectorType,"wrappedLevel",false,net.minecraft.class_1937.class,false);
            Field w=field(collectorType,"wrappedLevelsMap",true,Map.class,false),p=field(collectorType,"entityCache",true,Map.class,false);
            Class<?> renderer=type("com.anthonyhilyard.iceberg.renderer.CustomItemRenderer");
            var pairs=new Field[4][2];String[] names={"armorStand","horse","entity","blockEntity"},keys={"cachedArmorStandItem","cachedHorseArmorItem","cachedEntityItem","cachedBlockEntityItem"};
            for(int i=0;i<names.length;i++) {
                pairs[i][0]=field(renderer,names[i],true,i==3?class_2586.class:class_1297.class,true);
                pairs[i][1]=field(renderer,keys[i],true,com.mojang.datafixers.util.Pair.class,true);
            }
            // Resolve and validate all shapes before making any changes.
            previews=p;previewPairs=pairs;wrappers=w;
        }
        int count=WorldCacheReleaser.remove(map(previews),e->WorldCacheReleaser.anyObsolete(e.getValue(),o->oldOwner(o,current)));
        for(var pair:previewPairs)count+=WorldCacheReleaser.clearPair(pair[0],pair[1],o->oldOwner(o,current));
        return count+WorldCacheReleaser.remove(map(wrappers),e->{try{return oldWorld(e.getKey(),current);}catch(IllegalAccessException x){throw new IllegalStateException(x);}});
    }
    private int inventories(Object current)throws ReflectiveOperationException {
        if(inventoryWrappers==null) {
            Class<?> storage=type("net.fabricmc.fabric.impl.transfer.item.InventoryStorageImpl");
            Class<?> inventory=type("dev.shadowsoffire.apotheosis.ench.table.ApothEnchantTile$1"),tile=type("dev.shadowsoffire.apotheosis.ench.table.ApothEnchantTile");
            Field owner=field(inventory,"this$0",false,tile,false),cache=field(storage,"WRAPPERS",true,Map.class,false);
            zenithInventoryType=inventory;zenithOwner=owner;inventoryWrappers=cache;
        }
        return WorldCacheReleaser.remove(map(inventoryWrappers),e->{
            Object owner=e.getKey();
            try {
                if(owner instanceof class_1661 inventory)return oldOwner(inventory.field_7546,current);
                if(owner instanceof class_2586)return oldOwner(owner,current);
                if(zenithInventoryType.isInstance(owner))return oldOwner(zenithOwner.get(owner),current);
                return false;
            }catch(IllegalAccessException x){throw new IllegalStateException(x);}
        });
    }
    private int mi(Object current,Object player)throws ReflectiveOperationException {
        if(keyMap==null)keyMap=field(type("aztech.modern_industrialization.items.armor.MIKeyMap"),"HOLDING_UP",true,Map.class,false);
        return WorldCacheReleaser.remove(map(keyMap),e->e.getKey() instanceof class_746 p&&(p!=player||p.method_37908()!=current));
    }
    private int balm(Object current)throws ReflectiveOperationException {
        if(fallLocal==null) {
            var found=new ArrayList<Field>();
            for(Field f:class_1309.class.getDeclaredFields())if(Modifier.isStatic(f.getModifiers())&&f.getType()==ThreadLocal.class
                &&(f.getName().equals("currentFallEvent")||f.getName().endsWith("$currentFallEvent")))found.add(f);
            if(found.size()!=1)throw new IllegalStateException("Unknown merged Balm event field");
            Class<?> event=type("net.blay09.mods.balm.api.event.LivingFallEvent");Method getter=event.getMethod("getEntity");
            if(!class_1309.class.isAssignableFrom(getter.getReturnType()))throw new IllegalStateException("Unknown event owner getter");
            found.get(0).setAccessible(true);fallEntity=getter;fallType=event;fallLocal=found.get(0);
        }
        Object local=fallLocal.get(null);if(!(local instanceof ThreadLocal<?> thread))throw new IllegalStateException("Unknown event local shape");
        return WorldCacheReleaser.clearLocal(thread,event->{
            if(event.getClass()!=fallType)return false;
            try{return oldOwner(fallEntity.invoke(event),current);}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
        });
    }
    private void afterTick(class_310 client) {
        if(!changes.transition(client.field_1687,client.field_1724))return;
        int a=0,b=0,c=0,d=0;
        if(iceberg)try{a=iceberg(client.field_1687);}catch(ReflectiveOperationException|RuntimeException|LinkageError e){iceberg=false;warn("iceberg",e);}
        if(inventories)try{b=inventories(client.field_1687);}catch(ReflectiveOperationException|RuntimeException|LinkageError e){inventories=false;warn("inventories",e);}
        // MI's native HashMap is shared with the integrated-server thread. Do not add
        // cross-thread writes there; multiplayer clients have no local server writer.
        if(mi&&client.method_1576()==null)try{c=mi(client.field_1687,client.field_1724);}catch(ReflectiveOperationException|RuntimeException|LinkageError e){mi=false;warn("mi",e);}
        if(balm)try{d=balm(client.field_1687);}catch(ReflectiveOperationException|RuntimeException|LinkageError e){balm=false;warn("balm",e);}
        if(a+b+c+d>0)LoggerFactory.getLogger("Prominence compatibility").info("Client obsolete cache entries released: iceberg={}, inventories={}, mi={}, balm={}",a,b,c,d);
    }
    private static void warn(String module,Throwable e){LoggerFactory.getLogger("Prominence compatibility").warn("Disabling world-cache cleanup for {}; preserving native behavior",module,e);}
}
