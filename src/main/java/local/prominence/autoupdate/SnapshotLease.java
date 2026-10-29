package local.prominence.autoupdate;

/** The offer and channel chooser share one snapshot until the last modal closes. */
public final class SnapshotLease<T> {
    private T value;
    private final java.util.function.Consumer<T> dispose;
    private int owners=1;
    public SnapshotLease(T value,java.util.function.Consumer<T> dispose){this.value=java.util.Objects.requireNonNull(value);this.dispose=dispose;}
    public SnapshotLease<T> retain(){if(owners==0)throw new IllegalStateException("Closed snapshot");owners++;return this;}
    public T value(){return value;}
    public void release(){
        if(owners==0)throw new IllegalStateException("Snapshot released twice");
        if(--owners==0){T previous=value;value=null;dispose.accept(previous);}
    }
}
