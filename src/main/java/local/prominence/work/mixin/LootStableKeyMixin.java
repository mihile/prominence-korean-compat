package local.prominence.work.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.class_2960;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

/** Native key is text of a final immutable ID. Subclasses replacing getId keep their native path. */
@Mixin(targets="fzzyhmstrs.emi_loot.util.stack.EmiStackWithTitle",remap=false)
public abstract class LootStableKeyMixin {
    @Shadow @Final protected class_2960 id;
    @Unique private transient volatile String prominence$stableKey;
    @Unique private transient volatile byte prominence$keyEligibility;
    @Unique private boolean prominence$canMemoizeKey() {
        byte value=prominence$keyEligibility;
        if(value!=0)return value==1;
        boolean allowed=false;
        try {
            allowed=id!=null && id.getClass()==class_2960.class && getClass().getMethod("getId").getDeclaringClass().getName().equals("fzzyhmstrs.emi_loot.util.stack.EmiStackWithTitle");
        }catch(ReflectiveOperationException|LinkageError|SecurityException ignored){}
        prominence$keyEligibility=(byte)(allowed?1:2);return allowed;
    }
    @WrapMethod(method="getKey",remap=false)
    private Object prominence$reuseNativeKey(Operation<Object> original) {
        if(!prominence$canMemoizeKey())return original.call();
        String cached=prominence$stableKey;
        if(cached!=null)return cached;
        Object key=original.call();
        if(key instanceof String text)prominence$stableKey=text;
        return key;
    }
}
