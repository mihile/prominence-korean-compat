package local.prominence.work;

import me.cortex.voxy.common.voxelization.VoxelizedSection;

/** One scratch section per ingest task, released even when that task fails. */
public final class RemoteIngestScratch implements AutoCloseable {
    private static final ThreadLocal<RemoteIngestScratch> CURRENT=new ThreadLocal<>();
    private final RemoteIngestScratch previous;
    private VoxelizedSection section;
    private boolean closed;
    private RemoteIngestScratch(){previous=CURRENT.get();CURRENT.set(this);}
    public static RemoteIngestScratch begin(){return new RemoteIngestScratch();}
    public static VoxelizedSection acquire(){return acquire(VoxelizedSection::createEmpty);}
    public static VoxelizedSection acquire(java.util.function.Supplier<VoxelizedSection> original){
        RemoteIngestScratch scope=CURRENT.get();
        if(scope==null)return original.get();
        if(scope.section==null)scope.section=original.get();
        else scope.section.zero();
        return scope.section;
    }
    public void close(){
        if(closed)return;
        if(CURRENT.get()!=this)throw new IllegalStateException("Ingest scope closed out of order");
        closed=true;section=null;
        if(previous==null)CURRENT.remove();else CURRENT.set(previous);
    }
}
