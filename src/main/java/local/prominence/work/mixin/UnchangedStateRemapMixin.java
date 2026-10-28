package local.prominence.work.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import it.unimi.dsi.fastutil.ints.Int2ObjectRBTreeMap;
import java.util.Collection;
import java.util.function.Function;
import local.prominence.work.StateOrder;
import net.fabricmc.fabric.api.event.registry.RegistryIdRemapCallback;
import net.fabricmc.fabric.impl.registry.sync.trackers.StateIdTracker;
import net.minecraft.class_2361;
import net.minecraft.class_2378;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.*;

@Mixin(value=StateIdTracker.class,remap=false)
public abstract class UnchangedStateRemapMixin<T,S> {
    @Shadow @Final private class_2378<T> registry;
    @Shadow @Final private class_2361<S> stateList;
    @Shadow @Final private Function<T,Collection<S>> stateGetter;
    @Shadow private int currentHighestId;
    @WrapMethod(method="onRemap")
    private void prominence$unchanged(RegistryIdRemapCallback.RemapState<T> state,Operation<Void> original) {
        long started=System.nanoTime();
        var ordered=new Int2ObjectRBTreeMap<T>();int highest=0;
        for(T entry:registry) {int id=registry.method_10206(entry);ordered.put(id,entry);highest=Math.max(highest,id);}
        boolean same=StateOrder.identical(ordered.values(),stateGetter,stateList.method_10204(),stateList::method_10200);
        if(same) currentHighestId=highest;
        else original.call(state);
        LoggerFactory.getLogger("Prominence compatibility").info("Registry state remap: unchanged={}, states={}, elapsedMs={}",same,stateList.method_10204(),(System.nanoTime()-started)/1_000_000.0);
    }
}
