package local.prominence.memory;

/** Detects if the current call originates from MinecraftClient dimension transition (joinWorld). */
public final class DimensionChangeGate {
    private DimensionChangeGate() {}
    private static final StackWalker WALKER = StackWalker.getInstance();

    public static boolean isDimensionChange() {
        return WALKER.walk(frames -> frames.anyMatch(frame -> {
            String className = frame.getClassName();
            String methodName = frame.getMethodName();
            return className.contains("MixinMinecraft_PipelineManagement")
                || methodName.equals("method_18097")
                || methodName.equals("joinWorld");
        }));
    }
}
