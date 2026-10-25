package local.prominence.memory;

import net.minecraft.class_2586;
import net.minecraft.class_2818;
import net.minecraft.class_3218;

public interface BackpackUnloadCallbacks {
    void prominence$nativeChunkUnload(class_3218 world, class_2818 chunk);
    void prominence$nativeBlockEntityUnload(class_2586 entity, class_3218 world);
}
