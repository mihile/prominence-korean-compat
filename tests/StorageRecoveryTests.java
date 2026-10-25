package local.prominence.memory;

import com.google.gson.JsonParser;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import me.cortex.voxy.common.config.storage.rocksdb.RocksDBStorageBackend;
import org.rocksdb.RocksDBException;
import org.rocksdb.Status;

/** Deterministic scheduler + actual installed native DB failure; no game/world/GL. */
public final class StorageRecoveryTests {
    private static int checks;
    private static void check(boolean v,String m){checks++;if(!v)throw new AssertionError(m);}
    private static final class Events implements StorageOpenRetries.Events {
        final List<Integer> failures=new ArrayList<>();int exhausted,recovered;
        public void failed(int count,boolean stopped,RuntimeException failure){failures.add(count);if(stopped)exhausted++;}
        public void recovered(){recovered++;}
    }
    private static RuntimeException locked(){return new RuntimeException(new RocksDBException("Failed to create lock file: C:/test/LOCK: in use",new Status(Status.Code.IOError,Status.SubCode.None,null)));}
    public static void main(String[] args)throws Exception {
        var retries=new StorageOpenRetries();var owner=new Object();var world=new Object();var key="overworld";var events=new Events();var calls=new AtomicInteger();
        var fail=(java.util.function.Supplier<Object>)()->{calls.incrementAndGet();throw locked();};
        check(retries.open(owner,key,world,0,fail,VoxyStorageRecovery::isLockFailure,events)==null,"Lock failure escaped");
        check(retries.unavailable(owner,key,world),"Unavailable state missing");
        for(int i=0;i<2000;i++)retries.open(owner,key,world,1,fail,VoxyStorageRecovery::isLockFailure,events);
        check(calls.get()==1,"Repeated frames retried without backoff");
        check(retries.blocked(owner,key,world,249_999_999),"Retry too early");
        check(!retries.blocked(owner,key,world,250_000_000),"Due retry blocked");
        Object engine=new Object();check(retries.open(owner,key,world,250_000_000,()->engine,VoxyStorageRecovery::isLockFailure,events)==engine,"Recovery did not return native result");
        check(!retries.unavailable(owner,key,world)&&events.recovered==1,"Recovery state not cleared");
        var permanent=new StorageOpenRetries();var permanentEvents=new Events();long now=0;
        long[] delays={250_000_000,1_000_000_000,2_000_000_000L,4_000_000_000L,8_000_000_000L,16_000_000_000L};
        int before=calls.get();
        for(int i=0;i<7;i++){
            check(permanent.open(owner,key,world,now,fail,VoxyStorageRecovery::isLockFailure,permanentEvents)==null,"Permanent failure escaped");
            if(i<6){check(permanent.blocked(owner,key,world,now+delays[i]-1),"Backoff boundary wrong");now+=delays[i];}
        }
        for(int i=0;i<100;i++)permanent.open(owner,key,world,Long.MAX_VALUE,fail,VoxyStorageRecovery::isLockFailure,permanentEvents);
        check(calls.get()==before+7&&permanentEvents.exhausted==1,"Retry limit/one-time exhausted event wrong");
        check(permanent.open(owner,"end",world,now,()->engine,VoxyStorageRecovery::isLockFailure,events)==engine,"Different dimension blocked");
        check(permanent.open(owner,key,new Object(),now,()->engine,VoxyStorageRecovery::isLockFailure,events)==engine,"New world context cannot retry");
        RuntimeException unrelated=new IllegalStateException("real invariant");
        try{retries.open(owner,key,world,0,()->{throw unrelated;},VoxyStorageRecovery::isLockFailure,events);throw new AssertionError("Unrelated exception swallowed");}catch(RuntimeException e){check(e==unrelated,"Exception identity changed");}
        check(!VoxyStorageRecovery.isLockFailure(new RuntimeException(new RocksDBException("corrupt database",new Status(Status.Code.Corruption,Status.SubCode.None,null)))),"Corruption hidden");
        check(!VoxyStorageRecovery.isLockFailure(new RuntimeException(new RocksDBException("No space left",new Status(Status.Code.IOError,Status.SubCode.NoSpace,null)))),"Disk full hidden");
        var concurrent=new StorageOpenRetries();var entered=new CountDownLatch(1);var release=new CountDownLatch(1);
        concurrent.open(owner,key,world,0,fail,VoxyStorageRecovery::isLockFailure,events);
        ExecutorService executor=Executors.newSingleThreadExecutor();
        try{
            var future=executor.submit(()->concurrent.open(owner,key,world,250_000_000,()->{entered.countDown();try{release.await();}catch(InterruptedException e){throw new RuntimeException(e);}return engine;},VoxyStorageRecovery::isLockFailure,events));
            check(entered.await(3,TimeUnit.SECONDS),"Retry did not start");
            check(concurrent.open(owner,key,world,250_000_001,()->{throw new AssertionError("duplicate retry");},VoxyStorageRecovery::isLockFailure,events)==null,"Concurrent retry not gated");
            release.countDown();check(future.get(3,TimeUnit.SECONDS)==engine,"Concurrent success lost");
        }finally{release.countDown();executor.shutdownNow();}
        Object weakOwner=new Object(),weakWorld=new Object();var ownerRef=new WeakReference<>(weakOwner);var worldRef=new WeakReference<>(weakWorld);
        retries.open(weakOwner,"weak",weakWorld,0,fail,VoxyStorageRecovery::isLockFailure,events);weakOwner=null;weakWorld=null;
        for(int i=0;i<30&&(ownerRef.get()!=null||worldRef.get()!=null);i++){System.gc();Thread.sleep(10);}
        check(ownerRef.get()==null&&worldRef.get()==null,"Retry state retains a world/instance");retries.ownerCount();
        var pins=JsonParser.parseReader(new java.io.InputStreamReader(StorageRecoveryTests.class.getResourceAsStream("/prominence-storage-native-pins.json"),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
        var versions=Map.of("minecraft","1.20.1","voxy","0.2.20-beta+1.20.1-port.9","org_rocksdb_rocksdbjni","10.2.1");
        java.util.function.BiFunction<String,String,byte[]> bytes=(mod,path)->{try(var in=StorageRecoveryTests.class.getClassLoader().getResourceAsStream(path)){return in==null?null:in.readAllBytes();}catch(Exception e){return null;}};
        check(ShaderPreprocessGuard.verifyPins(pins,versions::get,bytes),"Installed storage pins mismatch");
        for(var mod:pins.entrySet())for(var pin:mod.getValue().getAsJsonObject().getAsJsonObject("classes").entrySet())check(!ShaderPreprocessGuard.verifyPins(pins,versions::get,(id,path)->path.equals(pin.getKey())?new byte[]{0}:bytes.apply(id,path)),"Modified native class accepted");
        check(!ShaderPreprocessGuard.verifyPins(pins,id->id.equals("voxy")?"different":versions.get(id),bytes),"Unknown version accepted");
        var path=Files.createTempDirectory("voxy-native-recovery-").resolve("한국어_store").toString();
        var holder=new RocksDBStorageBackend(path);var real=new StorageOpenRetries();var realEvents=new Events();
        try{
            holder.putIdMapping(123,ByteBuffer.wrap(new byte[]{11,22,33}));
            check(real.open(owner,key,world,0,()->new RocksDBStorageBackend(path),VoxyStorageRecovery::isLockFailure,realEvents)==null,"Real native conflict escaped");
            check(realEvents.failures.equals(List.of(1)),"Real failure not classified");
            holder.close();holder=null;
            var reopened=real.open(owner,key,world,250_000_000,()->new RocksDBStorageBackend(path),VoxyStorageRecovery::isLockFailure,realEvents);
            check(reopened!=null,"Actual DB did not recover");try{check(java.util.Arrays.equals(new byte[]{11,22,33},reopened.getIdMappingsData().get(123)),"Recovered database data changed");}finally{reopened.close();}
            check(realEvents.recovered==1,"Real recovery event missing");
        }finally{if(holder!=null)holder.close();}
        System.out.println("PASS: "+checks+" Voxy storage lock classification/backoff/cap/context/concurrency/weak lifetime/native pins/real RocksDB data recovery checks; no game/world/GL");
    }
}
