package local.prominence.memory;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.loader.api.FabricLoader;
import net.irisshaders.iris.helpers.StringPair;
import org.anarres.cpp.DefaultPreprocessorListener;
import org.slf4j.LoggerFactory;

public final class ShaderPreprocessBridge {
    private ShaderPreprocessBridge() {}
    private static final ThreadLocal<DefaultPreprocessorListener[]> CURRENT=new ThreadLocal<>();
    private static final class Holder {
        static final ShaderSourceDiskCache CACHE=new ShaderSourceDiskCache(FabricLoader.getInstance().getGameDir()
            .resolve(".cache/prominence-korean-compat/shader-preprocess-v1"),ShaderPreprocessGuard.cacheNamespace());
    }
    public static void observe(DefaultPreprocessorListener listener) {
        var scope=CURRENT.get();if(scope!=null)scope[0]=listener;
    }
    public static String preprocess(String source,Iterable<StringPair> defines,Operation<String> original) {
        // The measured caller supplies a List. Unknown/lazy iterables retain native iteration semantics.
        if(!(defines instanceof List<?>))return original.call(source,defines);
        return preprocessWithCache(Holder.CACHE,source,defines,original);
    }
    static String preprocessWithCache(ShaderSourceDiskCache cache,String source,Iterable<StringPair> defines,Operation<String> original) {
        if(!(defines instanceof List<?>))return original.call(source,defines);
        var keys=new ArrayList<ShaderSourceDiskCache.Define>();
        for(StringPair pair:defines) {
            if(pair==null)return original.call(source,defines);
            keys.add(new ShaderSourceDiskCache.Define(pair.key(),pair.value()));
        }
        return cache.preprocess(source,keys,()->{
            var previous=CURRENT.get();var scope=new DefaultPreprocessorListener[1];CURRENT.set(scope);
            try {
                String result=original.call(source,defines);var listener=scope[0];
                return new ShaderSourceDiskCache.Compilation(result,listener!=null&&listener.getErrors()==0&&listener.getWarnings()==0);
            }finally{if(previous==null)CURRENT.remove();else CURRENT.set(previous);}
        });
    }
    private static ShaderSourceDiskCache.Stats previous;
    public static synchronized void report(long nanos,Object dimension) {
        var current=Holder.CACHE.stats();var old=previous;previous=current;
        if(nanos<100_000_000L)return;
        LoggerFactory.getLogger("Prominence compatibility").info(
            "Client pipeline preparation: dimension={}, totalMs={}, shaderTextHitsDelta={}, missesDelta={}, nativePreprocessMsDelta={}, cacheReadMsDelta={}, pendingBytes={}, writesDelta={}, queueDroppedDelta={} (GPU compile is included in total, not text time)",
            dimension,nanos/1_000_000L,current.hits()-(old==null?0:old.hits()),current.misses()-(old==null?0:old.misses()),
            (current.nativeNanos()-(old==null?0:old.nativeNanos()))/1_000_000L,(current.readNanos()-(old==null?0:old.readNanos()))/1_000_000L,current.pendingBytes(),current.writes()-(old==null?0:old.writes()),current.droppedQueue()-(old==null?0:old.droppedQueue()));
    }
}
