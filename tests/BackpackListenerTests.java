package local.prominence.memory;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerBlockEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.minecraft.class_2586;
import net.minecraft.class_2818;
import net.minecraft.class_3218;

/** Uses real Fabric event invokers, but never constructs/starts a Minecraft world. */
public final class BackpackListenerTests {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    private static final class Listener implements BackpackUnloadCallbacks {
        final AtomicInteger chunks=new AtomicInteger(),entities=new AtomicInteger();
        public void prominence$nativeChunkUnload(class_3218 world,class_2818 chunk){chunks.incrementAndGet();}
        public void prominence$nativeBlockEntityUnload(class_2586 entity,class_3218 world){entities.incrementAndGet();}
    }
    private static int handlers(Event<?> event) throws Exception {
        Class<?> type=event.getClass();
        while(type!=null) {
            try {Field f=type.getDeclaredField("handlers");f.setAccessible(true);return java.lang.reflect.Array.getLength(f.get(event));}
            catch(NoSuchFieldException e){type=type.getSuperclass();}
        }
        throw new AssertionError("Native Fabric handler array absent");
    }
    private static WeakReference<Listener> temporary() {
        Listener listener=new Listener();BackpackUnloadDispatcher.track(listener);return new WeakReference<>(listener);
    }
    public static void main(String[] args) throws Exception {
        WeakListenerSet<Object> ordered=new WeakListenerSet<>();
        Object first=new Object(),second=new Object();ordered.add(first);ordered.add(second);ordered.add(first);
        check(ordered.snapshot().size()==2,"Duplicate identity retained");
        check(ordered.snapshot().get(0)==first&&ordered.snapshot().get(1)==second,"Registration order changed");
        int oldChunks=handlers(ServerChunkEvents.CHUNK_UNLOAD),oldEntities=handlers(ServerBlockEntityEvents.BLOCK_ENTITY_UNLOAD);
        Listener live=new Listener();BackpackUnloadDispatcher.track(live);BackpackUnloadDispatcher.track(live);
        check(handlers(ServerChunkEvents.CHUNK_UNLOAD)==oldChunks+1,"Not exactly one chunk dispatcher");
        check(handlers(ServerBlockEntityEvents.BLOCK_ENTITY_UNLOAD)==oldEntities+1,"Not exactly one block entity dispatcher");
        ServerChunkEvents.CHUNK_UNLOAD.invoker().onChunkUnload(null,null);
        ServerBlockEntityEvents.BLOCK_ENTITY_UNLOAD.invoker().onUnload(null,null);
        check(live.chunks.get()==1 && live.entities.get()==1,"Live callbacks lost or duplicated");
        List<Listener> survivors=new ArrayList<>();
        List<Thread> threads=new ArrayList<>();
        for(int t=0;t<4;t++) {
            List<Listener> group=new ArrayList<>();for(int i=0;i<60;i++)group.add(new Listener());survivors.addAll(group);
            Thread worker=new Thread(()->{for(Listener item:group){BackpackUnloadDispatcher.track(item);BackpackUnloadDispatcher.track(item);}});
            threads.add(worker);worker.start();
        }
        for(Thread worker:threads)worker.join();
        ServerChunkEvents.CHUNK_UNLOAD.invoker().onChunkUnload(null,null);
        ServerBlockEntityEvents.BLOCK_ENTITY_UNLOAD.invoker().onUnload(null,null);
        for(Listener listener:survivors)check(listener.chunks.get()==1&&listener.entities.get()==1,"Concurrent registration lost or duplicate callback");
        WeakReference<Listener> dead=temporary();
        for(int i=0;i<30&&dead.get()!=null;i++){System.gc();Thread.sleep(20);}
        check(dead.get()==null,"Global Fabric callbacks retain the obsolete target");
        ServerChunkEvents.CHUNK_UNLOAD.invoker().onChunkUnload(null,null);
        check(live.chunks.get()==3,"Collection of stale target stopped a live callback");
        check(handlers(ServerChunkEvents.CHUNK_UNLOAD)==oldChunks+1,"Per-instance handlers accumulated");
        check(handlers(ServerBlockEntityEvents.BLOCK_ENTITY_UNLOAD)==oldEntities+1,"Per-instance handlers accumulated");
        var fingerprint=BackpackListenerGuard.class.getDeclaredMethod("dependency",String.class,String.class);
        fingerprint.setAccessible(true);
        String path="net/fabricmc/fabric/api/event/lifecycle/v1/ServerChunkEvents.class";
        check((Boolean)fingerprint.invoke(null,path,"add011281682221a75075b4d1ff45ae280e84e77ac83c705fff0f071b5c73e85"),"Shipped Fabric API rejected");
        check(!(Boolean)fingerprint.invoke(null,path,"0".repeat(64)),"Modified API accepted");
        check(!(Boolean)fingerprint.invoke(null,"missing/event.class","0".repeat(64)),"Missing API accepted");
        String previous=System.getProperty("prominence.backpackWeakListeners");
        try {
            System.setProperty("prominence.backpackWeakListeners","false");
            BackpackListenerGuard guard=new BackpackListenerGuard();guard.onLoad("local.prominence.memory.mixin");
            check(!guard.shouldApplyMixin("BackpackBlockEntity","BackpackWeakUnloadMixin"),"Opt-out ignored");
        } finally {
            if(previous==null)System.clearProperty("prominence.backpackWeakListeners");else System.setProperty("prominence.backpackWeakListeners",previous);
        }
        Reference.reachabilityFence(live);Reference.reachabilityFence(survivors);
        System.out.println("PASS: "+checks+" real Fabric unload-event/weak-lifetime/concurrent-registration checks; no Minecraft world");
    }
}
