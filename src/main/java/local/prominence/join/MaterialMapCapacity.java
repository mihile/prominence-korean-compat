package local.prominence.join;

/** One scalar hint, no retained registry, world, tag, map, pipeline or GPU resource. */
public final class MaterialMapCapacity {
    private static final int LIMIT=131_072;
    private static volatile int expected=16;
    private MaterialMapCapacity() {}
    public static int expected() { return expected; }
    public static void completed(int size) { expected=Math.max(16,Math.min(LIMIT,size)); }
}
