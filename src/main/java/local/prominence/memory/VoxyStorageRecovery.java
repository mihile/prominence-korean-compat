package local.prominence.memory;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import me.cortex.voxy.client.VoxyClientInstance;
import me.cortex.voxy.common.world.WorldEngine;
import me.cortex.voxy.commonImpl.VoxyCommon;
import me.cortex.voxy.commonImpl.VoxyInstance;
import me.cortex.voxy.commonImpl.WorldIdentifier;
import net.minecraft.class_2561;
import net.minecraft.class_310;
import org.rocksdb.RocksDBException;
import org.rocksdb.Status;
import org.slf4j.LoggerFactory;

public final class VoxyStorageRecovery {
    private static final StorageOpenRetries RETRIES=new StorageOpenRetries();
    private static final StorageOpenRetries.Events EVENTS=new StorageOpenRetries.Events(){
        public void failed(int count,boolean exhausted,RuntimeException failure){
            var log=LoggerFactory.getLogger("Prominence compatibility");
            if(count==1)log.warn("Voxy client storage lock conflict; normal world rendering continues while LOD opening backs off",failure);
            if(exhausted){
                log.warn("Voxy client storage still locked after {} attempts; LOD paused for this world until a world/session change",count);
                var client=class_310.method_1551();
                client.execute(()->{if(client.field_1724!=null)client.field_1724.method_7353(class_2561.method_43471("prominence.storage_guard.paused"),false);});
            }
        }
        public void recovered(){LoggerFactory.getLogger("Prominence compatibility").info("Voxy client storage lock recovered; LOD opening resumed");}
    };
    private VoxyStorageRecovery(){}
    static boolean isLockFailure(RuntimeException failure) {
        Throwable t=failure;
        for(int i=0;t!=null&&i<16;i++,t=t.getCause())if(t instanceof RocksDBException rocks){
            var status=rocks.getStatus();String message=rocks.getMessage();
            return status!=null&&status.getCode()==Status.Code.IOError&&message!=null&&message.startsWith("Failed to create lock file:")&&message.contains("LOCK");
        }
        return false;
    }
    private static Object context(){return class_310.method_1551().field_1687;}
    public static WorldEngine open(VoxyInstance owner,WorldIdentifier key,boolean acquire,Operation<WorldEngine> original) {
        if(!(owner instanceof VoxyClientInstance))return original.call(key,acquire);
        return RETRIES.open(owner,key,context(),System.nanoTime(),()->original.call(key,acquire),VoxyStorageRecovery::isLockFailure,EVENTS);
    }
    public static boolean unavailable(WorldIdentifier key){
        var owner=VoxyCommon.getInstance();return owner instanceof VoxyClientInstance&&RETRIES.unavailable(owner,key,context());
    }
    public static boolean rendererBlocked(){
        var client=class_310.method_1551();if(client.field_1687==null)return false;
        var owner=VoxyCommon.getInstance();if(!(owner instanceof VoxyClientInstance))return false;
        var id=WorldIdentifier.of(client.field_1687);return id!=null&&RETRIES.blocked(owner,id,client.field_1687,System.nanoTime());
    }
}
