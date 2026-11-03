package local.prominence.memory;

/** A transient render/UI owner, never persistent game data. */
public interface RetainedClientOwner {
    Object prominence$retainedOwner();
    void prominence$releaseOwner();
}
