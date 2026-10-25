package local.prominence.backpackinline.mixin;
import local.prominence.backpackinline.HandshakeAccess;
import net.minecraft.class_3244;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
@Mixin(class_3244.class)
public abstract class InlineHandshakeMixin implements HandshakeAccess {
    @Unique private boolean prominence$inlineAgreed;
    public boolean prominence$inlineAgreed(){return prominence$inlineAgreed;}
    public void prominence$inlineAgree(){prominence$inlineAgreed=true;}
}
