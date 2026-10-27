package local.prominence.work;

/** Only the shader-enabled startup path waits for Voxy's existing Iris hook. */
public final class EarlyRendererGate {
    private EarlyRendererGate() {}
    public static void invoke(boolean shaderEnabled, Runnable original) {
        if (!shaderEnabled) original.run();
    }
}
