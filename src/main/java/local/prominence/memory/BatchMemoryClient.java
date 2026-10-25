package local.prominence.memory;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.slf4j.LoggerFactory;
public final class BatchMemoryClient implements ClientModInitializer {
    private final WorldReferencePruner transitions=new WorldReferencePruner();
    private boolean armorEnabled;
    public void onInitializeClient() {
        armorEnabled=BatchMemoryGuard.active("armor");
        if(BatchMemoryGuard.active("azure")||BatchMemoryGuard.active("armor"))ClientTickEvents.END_CLIENT_TICK.register(client->{
            if(!transitions.transition(client.field_1687,client.field_1724))return;
            int n=BatchMemoryGuard.active("azure")?AzureRendererTracker.release(client.field_1687):0;
            if(n>0)LoggerFactory.getLogger("Prominence compatibility").info("Client obsolete Azure renderer owners released: {}",n);
            if(armorEnabled&&BatchMemoryGuard.active("armor"))try {
                int armor=ArmorWorldContexts.release(client.field_1687);
                if(armor>0)LoggerFactory.getLogger("Prominence compatibility").info("Client obsolete armor renderer owners released: {}",armor);
            }catch(ReflectiveOperationException|RuntimeException e){armorEnabled=false;LoggerFactory.getLogger("Prominence compatibility").warn("Disabling armor owner cleanup after an unexpected shape",e);}
        });
    }
}
