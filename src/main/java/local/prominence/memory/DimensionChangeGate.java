package local.prominence.memory;

/** Detects if the current call originates from MinecraftClient dimension transition (joinWorld / respawn). */
public final class DimensionChangeGate {
    private DimensionChangeGate() {}

    public static boolean isDimensionChange() {
        StackTraceElement[] trace = Thread.currentThread().getStackTrace();
        for (StackTraceElement element : trace) {
            String className = element.getClassName();
            String methodName = element.getMethodName();
            if (methodName.contains("resetPipeline")
                    || methodName.contains("joinWorld")
                    || methodName.contains("method_18097")
                    || methodName.contains("method_1481")
                    || methodName.contains("onPlayerRespawn")
                    || methodName.contains("method_11120")
                    || className.contains("PipelineManagement")
                    || className.contains("ClientPlayNetworkHandler")) {
                return true;
            }
        }
        return false;
    }
}
