package local.prominence.memory;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerBlockEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;

/** One native event bridge per event; worlds own the block entities, this registry does not. */
public final class BackpackUnloadDispatcher {
    private static final WeakListenerSet<BackpackUnloadCallbacks> LISTENERS = new WeakListenerSet<>();
    static {
        ServerChunkEvents.CHUNK_UNLOAD.register((world, chunk) -> {
            for (BackpackUnloadCallbacks listener : LISTENERS.snapshot())
                listener.prominence$nativeChunkUnload(world, chunk);
        });
        ServerBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register((entity, world) -> {
            for (BackpackUnloadCallbacks listener : LISTENERS.snapshot())
                listener.prominence$nativeBlockEntityUnload(entity, world);
        });
    }
    private BackpackUnloadDispatcher() {}
    public static void track(BackpackUnloadCallbacks listener) { LISTENERS.add(listener); }
}
