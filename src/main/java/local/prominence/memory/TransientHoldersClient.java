package local.prominence.memory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentMap;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.class_1297;
import net.minecraft.class_310;
import net.minecraft.class_636;
import net.minecraft.class_638;
import net.minecraft.class_746;
import org.slf4j.LoggerFactory;

/** Optional, exact-guarded pruning after a world/player change, outside input/render callbacks. */
public final class TransientHoldersClient implements ClientModInitializer {
    private final WorldReferencePruner transitions=new WorldReferencePruner();
    private boolean porting,melodies;
    private Field captured,melodyInstance,melodyProgress;
    private Method eventPlayer;
    public void onInitializeClient() {
        porting=TransientHolderGuard.installed("porting_lib_entity");
        melodies=TransientHolderGuard.installed("immersive_melodies");
        LoggerFactory.getLogger("Prominence compatibility").info("Client obsolete interaction/music holders: porting={}, melodies={}",porting?"enabled":"disabled",melodies?"enabled":"disabled");
        if(porting||melodies)ClientTickEvents.END_CLIENT_TICK.register(this::afterTick);
    }
    private static Class<?> type(String name) throws ClassNotFoundException {
        return Class.forName(name,false,TransientHoldersClient.class.getClassLoader());
    }
    private static Field field(Class<?> type,String name) throws ReflectiveOperationException {
        Field f=type.getDeclaredField(name);f.setAccessible(true);return f;
    }
    private void afterTick(class_310 client) {
        if(!transitions.transition(client.field_1687,client.field_1724))return;
        int interactions=0,music=0;
        if(porting&&client.field_1761!=null)try {
            if(captured==null) {
                captured=field(class_636.class,"capturedEvent");
                if(!captured.getType().getName().equals("io.github.fabricators_of_create.porting_lib.entity.events.PlayerInteractionEvents$LeftClickBlock"))throw new IllegalStateException("Unknown captured-event shape");
                eventPlayer=type("io.github.fabricators_of_create.porting_lib.entity.events.PlayerEvents").getMethod("getPlayer");
            }
            interactions=TransientHolderPruner.clearCaptured(client.field_1761,captured,event->{
                try {
                    Object owner=eventPlayer.invoke(event);
                    // Preserve current input and all server-player contexts.
                    return owner instanceof class_746 p&&(p!=client.field_1724||p.method_37908()!=client.field_1687);
                }catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
            });
        }catch(ReflectiveOperationException|RuntimeException|LinkageError e){porting=false;warn("Porting Lib",e);}
        if(melodies)try {
            if(melodyInstance==null) {
                Class<?> owner=type("immersive_melodies.client.MelodyProgressManager");
                melodyInstance=field(owner,"INSTANCE");melodyProgress=field(owner,"progress");
            }
            Object manager=melodyInstance.get(null);
            if(manager!=null) {
                Object progress=melodyProgress.get(manager);
                if(!(progress instanceof ConcurrentMap<?,?>))throw new IllegalStateException("Unknown progress-map shape");
                @SuppressWarnings("unchecked") var map=(ConcurrentMap<Object,Object>)progress;
                music=TransientHolderPruner.removeObsolete(map,owner->owner instanceof class_1297 e&&e.method_37908() instanceof class_638&&e.method_37908()!=client.field_1687);
            }
        }catch(ReflectiveOperationException|RuntimeException|LinkageError e){melodies=false;warn("Immersive Melodies",e);}
        if(interactions+music>0)LoggerFactory.getLogger("Prominence compatibility").info("Client obsolete interaction/music holders released: interactions={}, music={}",interactions,music);
    }
    private static void warn(String module,Throwable error) {
        LoggerFactory.getLogger("Prominence compatibility").warn("Disabling obsolete holder cleanup for {}; retaining native behavior",module,error);
    }
}
