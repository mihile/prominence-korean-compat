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
    public static void report(long nanos,Object dimension) {
        if(nanos<100_000_000L)return;
        var s=Holder.CACHE.stats();
        LoggerFactory.getLogger("Prominence compatibility").info(
            "Client pipeline preparation: dimension={}, totalMs={}, shaderTextHits={}, misses={}, bypasses={}, writes={}, nativePreprocessMs={}, cacheReadMs={}, dirty={}, oversized={}, queueDropped={}, writeFailed={}, evicted={}, queued={}, pendingBytes={}",
            dimension,nanos/1_000_000L,s.hits(),s.misses(),s.bypasses(),s.writes(),s.nativeNanos()/1_000_000L,s.readNanos()/1_000_000L,s.skippedDirty(),s.skippedOversize(),s.droppedQueue(),s.writeFailures(),s.evictions(),s.queued(),s.pendingBytes());
    }
}
