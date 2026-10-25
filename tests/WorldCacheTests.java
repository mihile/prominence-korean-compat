package local.prominence.memory;

import com.google.common.collect.MapMaker;
import java.lang.ref.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Real Guava weak-value cycle, native byte/field shapes and pure owner-lifetime fixtures. */
public final class WorldCacheTests {
    static int checks;
    static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    static final class World {}
    static final class Tile {final World world;Object container;Tile(World w){world=w;}}
    static final class Inventory {final Tile owner;Inventory(Tile t){owner=t;}}
    static final class Wrapper {final Inventory inventory;Wrapper(Inventory i){inventory=i;}}
    static final class Entity {final Object world;Entity(Object w){world=w;}}
    static Object preview,previewKey;
    static byte[] raw(String name)throws Exception{try(var in=WorldCacheTests.class.getClassLoader().getResourceAsStream(name)){if(in==null)throw new AssertionError(name);return in.readAllBytes();}}
    static WeakReference<World> cycle(Map<Object,Object> map) {
        World world=new World();Tile tile=new Tile(world);Inventory inventory=new Inventory(tile);Wrapper wrapper=new Wrapper(inventory);tile.container=wrapper;map.put(inventory,wrapper);return new WeakReference<>(world);
    }
    static WeakReference<World> previousContexts(Map<Object,Object> worlds,Map<Object,Object> entities,Map<Object,Object> keys,ThreadLocal<Object> local) {
        World world=new World();Entity e=new Entity(world);worlds.put(world,new Entity(world));entities.put("old",List.of(e));keys.put(e,true);preview=e;previewKey="old-item";local.set(e);return new WeakReference<>(world);
    }
    static void collect(WeakReference<?> reference)throws Exception{for(int i=0;i<40&&reference.get()!=null;i++){System.gc();Thread.sleep(15);}}
    static ClassNode node(String path)throws Exception{var n=new ClassNode();new ClassReader(raw(path)).accept(n,ClassReader.SKIP_CODE);return n;}
    static void field(String path,String name,String desc,int flags)throws Exception {
        var n=node(path);var f=n.fields.stream().filter(v->v.name.equals(name)).findFirst().orElseThrow();check(f.desc.equals(desc)&&(f.access&flags)==flags,"Native field mismatch "+path+"."+name);
    }
    public static void main(String[]args)throws Exception {
        World current=new World(),server=new World();Entity active=new Entity(current),serverOwner=new Entity(server);
        var map=new HashMap<Object,Object>();map.put("current",active);map.put("server",serverOwner);map.put("old",new Entity(new World()));
        check(WorldCacheReleaser.remove(map,e->e.getKey().equals("old"))==1,"old entry retained");check(map.get("current")==active&&map.get("server")==serverOwner,"active/server entry lost");
        Object replacement=new Object();map.put("replace",new Object());
        check(WorldCacheReleaser.remove(map,e->{if(e.getKey().equals("replace")){map.put("replace",replacement);return true;}return false;})==0&&map.get("replace")==replacement,"replacement evicted");
        var concurrent=new ConcurrentHashMap<Object,Object>();concurrent.put("old",new Object());concurrent.put("current",active);
        check(WorldCacheReleaser.remove(concurrent,e->e.getKey().equals("old"))==1&&concurrent.get("current")==active,"concurrent map incorrect");
        check(WorldCacheReleaser.anyObsolete(List.of(active,serverOwner),o->o==active),"list detection failed");
        try{WorldCacheReleaser.anyObsolete("unknown",o->true);throw new AssertionError("unknown preview shape accepted");}catch(IllegalStateException expected){checks++;}
        Field owner=WorldCacheTests.class.getDeclaredField("preview"),key=WorldCacheTests.class.getDeclaredField("previewKey");
        preview=active;previewKey="active-key";check(WorldCacheReleaser.clearPair(owner,key,o->false)==0&&preview==active,"active preview changed");
        check(WorldCacheReleaser.clearPair(owner,key,o->{preview=serverOwner;return true;})==0&&preview==serverOwner&&previewKey.equals("active-key"),"reentrant preview replacement lost");
        check(WorldCacheReleaser.clearPair(owner,key,o->true)==1&&preview==null&&previewKey==null,"preview pair not reset");
        ThreadLocal<Object> local=new ThreadLocal<>();local.set(active);check(WorldCacheReleaser.clearLocal(local,o->false)==0&&local.get()==active,"active fall event lost");
        check(WorldCacheReleaser.clearLocal(local,o->{local.set(serverOwner);return true;})==0&&local.get()==serverOwner,"new event removed");
        check(WorldCacheReleaser.clearLocal(local,o->true)==1&&local.get()==null,"old fall event retained");local.remove();
        var wrappers=new MapMaker().weakValues().<Object,Object>makeMap();WeakReference<World> weak=cycle(wrappers);
        System.gc();check(weak.get()!=null,"native weak-value cycle was not reproduced");
        check(WorldCacheReleaser.remove(wrappers,e->e.getKey() instanceof Inventory)==1,"cycle entry not evicted");collect(weak);check(weak.get()==null,"evicted wrapper still owns world");Reference.reachabilityFence(wrappers);
        var worlds=new HashMap<Object,Object>();var entities=new HashMap<Object,Object>();var keys=new HashMap<Object,Object>();
        worlds.put(current,new Entity(current));entities.put("current",List.of(active));keys.put(serverOwner,true);
        WeakReference<World> old=previousContexts(worlds,entities,keys,local);
        check(WorldCacheReleaser.remove(entities,e->e.getKey().equals("old"))==1,"entity cache not evicted");
        check(WorldCacheReleaser.remove(keys,e->e.getKey()!=serverOwner)==1,"old key not evicted");
        check(WorldCacheReleaser.clearPair(owner,key,o->true)==1,"cached renderer owner retained");
        check(WorldCacheReleaser.clearLocal(local,o->true)==1,"old event retained");
        check(WorldCacheReleaser.remove(worlds,e->e.getKey()!=current)==1,"old wrapper retained");
        collect(old);check(old.get()==null,"live cache family still owns old world");
        check(worlds.containsKey(current)&&entities.get("current")!=null&&keys.containsKey(serverOwner),"current/server contexts damaged");Reference.reachabilityFence(worlds);
        var changes=new WorldReferencePruner();check(!changes.transition(null,null),"empty initial transition");check(changes.transition(current,active),"initial world skipped");check(!changes.transition(current,active),"steady state triggers scan");check(changes.transition(current,new Object()),"respawn skipped");check(changes.transition(null,null),"disconnect skipped");
        for(String module:List.of("iceberg","inventories","mi","balm")) {
            var versions=new HashMap<String,String>();var bytes=new HashMap<String,byte[]>();
            for(var pin:WorldCacheGuard.pins(module)){versions.put(pin.mod(),pin.version());for(String p:pin.classes().keySet())bytes.put(pin.mod()+"/"+p,raw(p));}
            check(WorldCacheGuard.matches(module,versions,bytes),"native pins rejected: "+module);
            for(String mod:List.copyOf(versions.keySet())){String v=versions.put(mod,"unknown");check(!WorldCacheGuard.matches(module,versions,bytes),"new version accepted");versions.put(mod,v);}
            for(String p:List.copyOf(bytes.keySet())){byte[] b=bytes.get(p),bad=b.clone();bad[bad.length/2]^=1;bytes.put(p,bad);check(!WorldCacheGuard.matches(module,versions,bytes),"modified class accepted");bytes.remove(p);check(!WorldCacheGuard.matches(module,versions,bytes),"missing class accepted");bytes.put(p,b);}
        }
        check(!WorldCacheGuard.matches("unknown",Map.of(),Map.of()),"unknown module accepted");
        field("com/anthonyhilyard/iceberg/util/EntityCollector.class","wrappedLevelsMap","Ljava/util/Map;",Opcodes.ACC_STATIC);
        field("com/anthonyhilyard/iceberg/util/EntityCollector.class","entityCache","Ljava/util/Map;",Opcodes.ACC_STATIC);
        field("com/anthonyhilyard/iceberg/util/EntityCollector.class","wrappedLevel","Lnet/minecraft/class_1937;",Opcodes.ACC_FINAL);
        String renderer="com/anthonyhilyard/iceberg/renderer/CustomItemRenderer.class";
        for(String f:List.of("armorStand","horse","entity","blockEntity"))check(node(renderer).fields.stream().anyMatch(v->v.name.equals(f)&&(v.access&Opcodes.ACC_STATIC)!=0&&(v.access&Opcodes.ACC_FINAL)==0),"unknown renderer field "+f);
        for(String f:List.of("cachedArmorStandItem","cachedHorseArmorItem","cachedEntityItem","cachedBlockEntityItem"))field(renderer,f,"Lcom/mojang/datafixers/util/Pair;",Opcodes.ACC_STATIC);
        field("net/fabricmc/fabric/impl/transfer/item/InventoryStorageImpl.class","WRAPPERS","Ljava/util/Map;",Opcodes.ACC_STATIC);
        field("dev/shadowsoffire/apotheosis/ench/table/ApothEnchantTile$1.class","this$0","Ldev/shadowsoffire/apotheosis/ench/table/ApothEnchantTile;",Opcodes.ACC_FINAL);
        field("aztech/modern_industrialization/items/armor/MIKeyMap.class","HOLDING_UP","Ljava/util/Map;",Opcodes.ACC_STATIC);
        field("net/blay09/mods/balm/mixin/LivingEntityMixin.class","currentFallEvent","Ljava/lang/ThreadLocal;",Opcodes.ACC_STATIC|Opcodes.ACC_FINAL);
        check(node("net/blay09/mods/balm/api/event/LivingFallEvent.class").methods.stream().anyMatch(m->m.name.equals("getEntity")&&m.desc.equals("()Lnet/minecraft/class_1309;")),"unknown native event getter");
        System.out.println("PASS: "+checks+" world-cache lifecycle/native shape/pin checks; native Guava key-value cycle reproduced and collected; active/server and replacement contexts preserved; no Minecraft world launched.");
    }
}
