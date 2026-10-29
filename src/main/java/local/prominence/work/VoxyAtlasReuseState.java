package local.prominence.work;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class VoxyAtlasReuseState {
    private static final Logger LOGGER = LoggerFactory.getLogger("Prominence compatibility");
    private static volatile boolean lastAcquiredWasReused;
    private static volatile int lastAcquiredPixelCount;
    private static volatile int activeGlId = -1;
    private static volatile boolean dirty;
    private static volatile int reuseCount;

    private VoxyAtlasReuseState() {}

    public static void onAcquire(boolean reused, int pixelCount) {
        lastAcquiredWasReused = reused;
        lastAcquiredPixelCount = pixelCount;
    }

    public static boolean shouldSkipReadback(int glId, int pixelLength) {
        if (!lastAcquiredWasReused) {
            return false;
        }
        if (dirty) {
            return false;
        }
        if (activeGlId != -1 && activeGlId != glId) {
            return false;
        }
        if (lastAcquiredPixelCount != pixelLength || pixelLength <= 0) {
            return false;
        }
        return true;
    }

    public static void logReuse(int glId, int pixelLength) {
        activeGlId = glId;
        reuseCount++;
        LOGGER.info("Voxy model atlas reused from previous dimension: glId={}, bytes={}, totalReuses={}; bypassed 268MB GPU PBO readback and fence wait",
            glId, (long) pixelLength * 4L, reuseCount);
    }

    public static void onTextureInstalled(int glId) {
        activeGlId = glId;
        dirty = false;
    }

    public static void markDirty() {
        dirty = true;
        activeGlId = -1;
    }
}
