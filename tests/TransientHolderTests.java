package local.prominence.memory;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

/** Real native progress-map storage, retention and stale-only behavior; no game/world startup. */
public final class TransientHolderTests {
    private static int checks;
    private static void check(boolean value,String text){checks++;if(!value)throw new AssertionError(text);}
    private static final class Owner {final Object world;Owner(Object world){this.world=world;}}
    private static final class Event {final Owner player;Event(Owner player){this.player=player;}}
    private static final class Holder {private Event capturedEvent;}
    private static final Field CAPTURED;
    static {try{CAPTURED=Holder.class.getDeclaredField("capturedEvent");CAPTURED.setAccessible(true);}catch(Exception e){throw new ExceptionInInitializerError(e);}}
    private static byte[] raw(String name)throws Exception {
        try(var in=TransientHolderTests.class.getClassLoader().getResourceAsStream(name)) {
            if(in==null)throw new AssertionError("Missing installed native dependency "+name);return in.readAllBytes();
        }
    }
    private static WeakReference<Object> populateThenPrune(Holder holder,ConcurrentMap<Object,Object> progress)throws Exception {
        Object oldWorld=new Object();Owner old=new Owner(oldWorld);holder.capturedEvent=new Event(old);progress.put(old,new Object());
        var weak=new WeakReference<>(oldWorld);
        TransientHolderPruner.clearCaptured(holder,CAPTURED,event->true);
        TransientHolderPruner.removeObsolete(progress,key->true);return weak;
    }
    @SuppressWarnings("unchecked")
    public static void main(String[] args)throws Exception {
        Object oldWorld=new Object(),currentWorld=new Object();Owner old=new Owner(oldWorld),active=new Owner(currentWorld);
        Holder holder=new Holder();Event activeEvent=new Event(active);holder.capturedEvent=activeEvent;
        check(TransientHolderPruner.clearCaptured(holder,CAPTURED,event->((Event)event).player!=active)==0,"Current block input cleared");
        check(holder.capturedEvent==activeEvent,"Current event replaced");
        Event previous=new Event(old);holder.capturedEvent=previous;
        check(TransientHolderPruner.clearCaptured(holder,CAPTURED,event->((Event)event).player!=active)==1,"Obsolete input retained");
        check(holder.capturedEvent==null,"Old player still held");
        check(TransientHolderPruner.clearCaptured(holder,CAPTURED,event->true)==0,"Empty input changed");
        holder.capturedEvent=previous;
        check(TransientHolderPruner.clearCaptured(holder,CAPTURED,event->{holder.capturedEvent=activeEvent;return true;})==0,"Reentrant replacement cleared");
        check(holder.capturedEvent==activeEvent,"New input lost");
        // A new native capture after a transition is retained, not permanently disabled.
        holder.capturedEvent=new Event(active);
        check(TransientHolderPruner.clearCaptured(holder,CAPTURED,event->((Event)event).player!=active)==0,"Next interaction disabled");

        Class<?> managerType=Class.forName("immersive_melodies.client.MelodyProgressManager",false,TransientHolderTests.class.getClassLoader());
        Object manager=managerType.getConstructor().newInstance();Field field=managerType.getDeclaredField("progress");field.setAccessible(true);
        Object nativeMap=field.get(manager);check(nativeMap instanceof ConcurrentMap,"Installed progress map is not concurrent");
        ConcurrentMap<Object,Object> progress=(ConcurrentMap<Object,Object>)nativeMap;
        Object nativeProgress=new Object(),oldProgress=new Object();progress.put(active,nativeProgress);progress.put(old,oldProgress);
        check(TransientHolderPruner.removeObsolete(progress,key->((Owner)key).world!=currentWorld)==1,"Wrong music removal count");
        check(progress.get(active)==nativeProgress&&!progress.containsKey(old),"Current music progress changed");
        check(TransientHolderPruner.removeObsolete(progress,key->((Owner)key).world!=currentWorld)==0,"Repeated prune changed current music");

        // An asynchronous native replacement must survive the compare-and-remove.
        progress.clear();progress.put(old,oldProgress);Object replacement=new Object();
        CountDownLatch inspected=new CountDownLatch(1),replaced=new CountDownLatch(1);
        Thread updater=new Thread(()->{try{inspected.await();progress.put(old,replacement);replaced.countDown();}catch(InterruptedException e){throw new AssertionError(e);}});
        updater.start();int changed=TransientHolderPruner.removeObsolete(progress,key->{inspected.countDown();try{replaced.await();}catch(InterruptedException e){throw new AssertionError(e);}return true;});updater.join();
        check(changed==0&&progress.get(old)==replacement,"Concurrent music update removed");
        check(TransientHolderPruner.removeObsolete(progress,key->true)==1&&progress.isEmpty(),"Disconnect did not unlink old music");
        holder.capturedEvent=null;var world=populateThenPrune(holder,progress);
        for(int i=0;i<30&&world.get()!=null;i++){System.gc();Thread.sleep(20);}
        check(world.get()==null,"Live holder/map retains the obsolete world after pruning");
        Reference.reachabilityFence(manager);Reference.reachabilityFence(progress);Reference.reachabilityFence(holder);

        for(String id:new String[]{"porting_lib_entity","immersive_melodies"}) {
            String version=id.equals("porting_lib_entity")?"2.3.13+1.20.1":"0.5.0+1.20.1";
            Map<String,byte[]> bytes=new HashMap<>();for(String name:TransientHolderGuard.pinned(id).keySet())bytes.put(name,raw(name));
            check(TransientHolderGuard.matches(id,version,bytes),"Native fingerprints rejected");
            check(!TransientHolderGuard.matches(id,"unknown",bytes),"Unreviewed version accepted");
            for(String name:java.util.List.copyOf(bytes.keySet())) {
                byte[] original=bytes.get(name),changedBytes=original.clone();changedBytes[changedBytes.length/2]^=1;
                bytes.put(name,changedBytes);check(!TransientHolderGuard.matches(id,version,bytes),"Modified native class accepted");
                bytes.remove(name);check(!TransientHolderGuard.matches(id,version,bytes),"Missing native class accepted");
                bytes.put(name,original);check(TransientHolderGuard.matches(id,version,bytes),"Restored native class rejected");
            }
        }
        check(!TransientHolderGuard.matches("other","",Map.of()),"Unknown target accepted");
        var mixin=new ClassNode();new ClassReader(raw("io/github/fabricators_of_create/porting_lib/entity/mixin/client/MultiPlayerGameModeMixin.class")).accept(mixin,0);
        check(mixin.fields.stream().anyMatch(f->f.name.equals("capturedEvent")&&f.desc.equals("Lio/github/fabricators_of_create/porting_lib/entity/events/PlayerInteractionEvents$LeftClickBlock;")),"Captured native field shape mismatch");
        var playerEvent=new ClassNode();new ClassReader(raw("io/github/fabricators_of_create/porting_lib/entity/events/PlayerEvents.class")).accept(playerEvent,0);
        check(playerEvent.methods.stream().anyMatch(m->m.name.equals("getPlayer")&&m.desc.equals("()Lnet/minecraft/class_1657;")),"Native event owner getter mismatch");
        System.out.println("PASS: "+checks+" obsolete input/native music map preservation, concurrent replacement, actual world collection, native shape/fingerprint checks; no Minecraft world");
    }
}
