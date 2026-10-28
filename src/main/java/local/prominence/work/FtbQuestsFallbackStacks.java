package local.prominence.work;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.class_1792;
import net.minecraft.class_1799;
import net.minecraft.class_7923;

/**
 * Provides an instantaneous, safe snapshot of registry item stacks for FTB Quests item filtering.
 * Prevents main render thread stalls caused by synchronous creative tab rebuilding while background
 * creative indexing or monitor locks are active.
 */
public final class FtbQuestsFallbackStacks {
    private static volatile List<class_1799> CACHED;

    private FtbQuestsFallbackStacks() {}

    public static List<class_1799> getRegistryStacks() {
        List<class_1799> cached = CACHED;
        if (cached != null) return cached;
        synchronized (FtbQuestsFallbackStacks.class) {
            if (CACHED != null) return CACHED;
            List<class_1799> list = new ArrayList<>();
            try {
                for (class_1792 item : class_7923.field_41178) {
                    if (item != null) {
                        class_1799 stack = new class_1799(item);
                        if (!stack.method_7960()) {
                            list.add(stack);
                        }
                    }
                }
            } catch (Throwable ignored) {}
            CACHED = Collections.unmodifiableList(list);
            return CACHED;
        }
    }

    public static void clear() {
        synchronized (FtbQuestsFallbackStacks.class) {
            CACHED = null;
        }
    }
}
