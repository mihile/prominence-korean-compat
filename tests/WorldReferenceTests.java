package local.prominence.memory;

import java.lang.ref.WeakReference;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class WorldReferenceTests {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    private static final class Entity { final Object world;Entity(Object world){this.world=world;} }
    private static final class Sound { final Entity entity;boolean stopped;Sound(Entity entity){this.entity=entity;} }
    private static WeakReference<Object>[] populate(WorldReferencePruner tracker) {
        Object world=new Object(),player=new Object();tracker.transition(world,player);
        @SuppressWarnings("unchecked") WeakReference<Object>[] refs=new WeakReference[]{new WeakReference<>(world),new WeakReference<>(player)};
        return refs;
    }
    private static void assertCollected(WeakReference<?> ref) throws Exception {
        for(int i=0;i<30&&ref.get()!=null;i++){System.gc();Thread.sleep(20);}
        check(ref.get()==null,"Pruner retains obsolete world/player");
    }
    private static void shape(String type,String name,boolean isStatic,String expectedType) throws Exception {
        Class<?> owner=Class.forName(type,false,WorldReferenceTests.class.getClassLoader());
        var f=owner.getDeclaredField(name);
        check(Modifier.isStatic(f.getModifiers())==isStatic,"Wrong static field: "+name);
        check(f.getType().getName().equals(expectedType),"Wrong field type: "+name);
    }
    public static void main(String[] args) throws Exception {
        WorldReferencePruner tracker=new WorldReferencePruner();
        Object firstWorld=new Object(),secondWorld=new Object(),firstPlayer=new Object(),secondPlayer=new Object();
        check(!tracker.transition(null,null),"Title should not eagerly initialize mod sound classes");
        check(tracker.transition(firstWorld,firstPlayer),"First world not detected");
        for(int i=0;i<100;i++)check(!tracker.transition(firstWorld,firstPlayer),"Stable world/player rescan");
        check(tracker.transition(secondWorld,firstPlayer),"World transition not detected");
        check(tracker.transition(secondWorld,secondPlayer),"Respawn/player replacement not detected");
        check(tracker.transition(null,null),"Disconnect not detected");
        check(!tracker.transition(null,null),"Repeated disconnect rescan");
        WorldReferencePruner retainedTracker=new WorldReferencePruner();
        var refs=populate(retainedTracker);assertCollected(refs[0]);assertCollected(refs[1]);
        java.lang.ref.Reference.reachabilityFence(retainedTracker);
        Map<Entity,Sound> sounds=new WeakHashMap<>();
        Entity active=new Entity(secondWorld),obsolete=new Entity(firstWorld);
        Sound activeSound=new Sound(active),obsoleteSound=new Sound(obsolete);
        sounds.put(active,activeSound);sounds.put(obsolete,obsoleteSound);
        check(WorldReferencePruner.removeStale(sounds,e->e!=null&&e.world==secondWorld,s->s.stopped=true)==1,"Stale sound not removed");
        check(!activeSound.stopped&&obsoleteSound.stopped,"Active ambience stopped");
        check(sounds.get(active)==activeSound&&!sounds.containsKey(obsolete),"Wrong map entry removed");
        check(WorldReferencePruner.removeStale(sounds,e->false,s->s.stopped=true)==1,"Disconnect should drop all former-world sounds");
        check(activeSound.stopped&&sounds.isEmpty(),"Disconnect sound still retained");
        Map<Object,Object> reentrant=new HashMap<>();Object key=new Object(),old=new Object(),replacement=new Object();reentrant.put(key,old);
        AtomicInteger stopped=new AtomicInteger();
        WorldReferencePruner.removeStale(reentrant,k->false,s->{check(!reentrant.containsKey(key),"Unlink must precede reentrant stop");reentrant.put(key,replacement);stopped.incrementAndGet();});
        check(reentrant.get(key)==replacement&&stopped.get()==1,"New reentrant sound deleted");
        check(!WorldReferencePruner.obsoleteOwner(firstPlayer,firstWorld,firstPlayer,firstWorld),"Active owner marked stale");
        check(WorldReferencePruner.obsoleteOwner(firstPlayer,firstWorld,secondPlayer,firstWorld),"Old player not stale");
        check(WorldReferencePruner.obsoleteOwner(firstPlayer,firstWorld,firstPlayer,secondWorld),"Old world not stale");
        check(WorldReferencePruner.obsoleteOwner(firstPlayer,firstWorld,null,null),"Disconnected owner not stale");
        shape("dev.obscuria.aquamirae.client.BlizzardContext","sound",true,"dev.obscuria.aquamirae.client.sound.BlizzardSoundInstance");
        shape("dev.obscuria.aquamirae.client.BlizzardContext","active",true,"boolean");
        shape("dev.obscuria.aquamirae.client.sound.BlizzardSoundInstance","player",false,"net.minecraft.class_1657");
        shape("com.telepathicgrunt.the_bumblezone.client.LivingEntityFlyingSoundInstance","activeSounds",true,"java.util.WeakHashMap");
        shape("net.lyof.sortilege.mixin.client.ItemStackMixin","sorti_player",true,"net.minecraft.class_1657");
        shape("net.lyof.sortilege.mixin.client.ItemStackMixin","sorti_stack",true,"net.minecraft.class_1799");
        System.out.println("PASS: "+checks+" world/player lifetime, active ambience, reentrant stop and actual native field-shape checks; no world/Game/OpenGL");
    }
}
