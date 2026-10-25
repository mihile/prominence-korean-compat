package worldcachefixture;
import java.lang.reflect.*;
import java.util.*;
import net.minecraft.*;
import com.mojang.datafixers.util.Pair;
import com.anthonyhilyard.iceberg.util.EntityCollector;
import com.anthonyhilyard.iceberg.renderer.CustomItemRenderer;
import aztech.modern_industrialization.items.armor.MIKeyMap;
import dev.shadowsoffire.apotheosis.ench.table.ApothEnchantTile;
import net.fabricmc.fabric.impl.transfer.item.InventoryStorageImpl;
import net.blay09.mods.balm.api.event.LivingFallEvent;

/** Data-only stub contexts; runs the production callback/field resolution, never Minecraft. */
public final class Probe {
    static int checks;
    static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    public static void main(String[]args)throws Exception {
        Class<?> code=Class.forName("local.prominence.memory.WorldCacheCleanupClient");Object production=code.getConstructor().newInstance();
        for(String name:List.of("iceberg","inventories","mi","balm")){Field f=code.getDeclaredField(name);f.setAccessible(true);f.setBoolean(production,true);}
        Method tick=code.getDeclaredMethod("afterTick",class_310.class);tick.setAccessible(true);
        var client=new class_310();var old=new class_638();var current=new class_638();var server=new class_1937();
        var active=new class_746(current);var oldPlayer=new class_746(old);var serverPlayer=new class_1657(server);client.field_1687=current;client.field_1724=active;
        EntityCollector oldCollector=new EntityCollector(old),activeCollector=new EntityCollector(current),serverCollector=new EntityCollector(server);
        EntityCollector.wrappedLevelsMap.put(old,oldCollector);EntityCollector.wrappedLevelsMap.put(current,activeCollector);EntityCollector.wrappedLevelsMap.put(server,serverCollector);
        var oldPreview=new class_1297(oldCollector);var activePreview=new class_1297(activeCollector);EntityCollector.entityCache.put("old",List.of(oldPreview));EntityCollector.entityCache.put("active",List.of(activePreview));
        CustomItemRenderer.armorStand=oldPreview;CustomItemRenderer.cachedArmorStandItem=Pair.of("old","old");CustomItemRenderer.entity=activePreview;CustomItemRenderer.cachedEntityItem=Pair.of("active","active");
        var oldTile=new ApothEnchantTile(old);var activeTile=new ApothEnchantTile(current);var serverTile=new ApothEnchantTile(server);
        var wrappers=InventoryStorageImpl.WRAPPERS;wrappers.put(oldTile.inventory,oldTile.container);wrappers.put(activeTile.inventory,activeTile.container);wrappers.put(serverTile.inventory,serverTile.container);
        class_1661 oldInv=new class_1661(oldPlayer),activeInv=new class_1661(active),serverInv=new class_1661(serverPlayer);var storageOwners=List.of(new Object(),new Object(),new Object());wrappers.put(oldInv,storageOwners.get(0));wrappers.put(activeInv,storageOwners.get(1));wrappers.put(serverInv,storageOwners.get(2));
        MIKeyMap.HOLDING_UP.put(oldPlayer,true);MIKeyMap.HOLDING_UP.put(active,true);MIKeyMap.HOLDING_UP.put(serverPlayer,false);
        var event=new LivingFallEvent(new class_1309(old));class_1309.currentFallEvent.set(event);
        var ready=new java.util.concurrent.CountDownLatch(1);var release=new java.util.concurrent.CountDownLatch(1);var failure=new java.util.concurrent.atomic.AtomicReference<Throwable>();
        Thread other=new Thread(()->{try{class_1309.currentFallEvent.set(new LivingFallEvent(new class_1309(server)));ready.countDown();release.await();check(class_1309.currentFallEvent.get()!=null,"other thread local removed");}catch(Throwable e){failure.set(e);}finally{class_1309.currentFallEvent.remove();}});other.start();ready.await();
        try{tick.invoke(production,client);}finally{release.countDown();other.join();}if(failure.get()!=null)throw new AssertionError(failure.get());
        check(!EntityCollector.wrappedLevelsMap.containsKey(old)&&EntityCollector.wrappedLevelsMap.get(current)==activeCollector&&EntityCollector.wrappedLevelsMap.get(server)==serverCollector,"wrapper current/server preservation");
        check(!EntityCollector.entityCache.containsKey("old")&&EntityCollector.entityCache.containsKey("active"),"preview-list cleanup");
        check(CustomItemRenderer.armorStand==null&&CustomItemRenderer.cachedArmorStandItem==null&&CustomItemRenderer.entity==activePreview,"renderer/cache pair handling");
        check(!wrappers.containsKey(oldTile.inventory)&&wrappers.containsKey(activeTile.inventory)&&wrappers.containsKey(serverTile.inventory),"Zenith owner classification");
        check(!wrappers.containsKey(oldInv)&&wrappers.containsKey(activeInv)&&wrappers.containsKey(serverInv),"vanilla inventory owner classification");
        check(oldTile.container!=null&&activeTile.container!=null,"tile data/container mutated");
        check(!MIKeyMap.HOLDING_UP.containsKey(oldPlayer)&&MIKeyMap.HOLDING_UP.get(active)&&!MIKeyMap.HOLDING_UP.get(serverPlayer),"MI current/server handling");
        check(class_1309.currentFallEvent.get()==null,"Balm event not released");
        // Steady state does not scan; a later transition releases the same obsolete cache entry.
        EntityCollector.wrappedLevelsMap.put(old,oldCollector);tick.invoke(production,client);check(EntityCollector.wrappedLevelsMap.containsKey(old),"steady state scanned map");
        client.field_1724=new class_746(current);tick.invoke(production,client);check(!EntityCollector.wrappedLevelsMap.containsKey(old),"respawn skipped cleanup");
        class_746 anotherOld=new class_746(old);MIKeyMap.HOLDING_UP.put(anotherOld,true);client.server=new class_1132();client.field_1724=new class_746(current);tick.invoke(production,client);check(MIKeyMap.HOLDING_UP.containsKey(anotherOld),"shared integrated-server HashMap changed");client.server=null;
        var activeFall=new LivingFallEvent(new class_1309(current));class_1309.currentFallEvent.set(activeFall);client.field_1724=new class_746(current);tick.invoke(production,client);check(class_1309.currentFallEvent.get()==activeFall,"current event lost");
        client.field_1687=null;client.field_1724=null;tick.invoke(production,client);check(class_1309.currentFallEvent.get()==null,"disconnect fall retained");
        check(EntityCollector.wrappedLevelsMap.size()==1&&EntityCollector.wrappedLevelsMap.containsKey(server),"disconnect removed server wrapper");
        check(wrappers.containsKey(serverTile.inventory)&&wrappers.containsKey(serverInv),"disconnect removed server inventories");
        java.lang.ref.Reference.reachabilityFence(storageOwners);
        System.out.println("PASS: "+checks+" production client callback/real reflective adapter checks with data-only stub contexts; steady/respawn/disconnect, wrapper cycles, current/server/integrated-server and thread-local isolation; no game/world/GL.");
    }
}
