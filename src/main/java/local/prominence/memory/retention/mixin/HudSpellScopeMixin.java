package local.prominence.memory.retention.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import local.prominence.memory.HudSpellReadScope;
import net.minecraft.class_332;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(targets="net.spell_engine.client.gui.HudRenderHelper",remap=false)
public abstract class HudSpellScopeMixin {
    @WrapMethod(method="render(Lnet/minecraft/class_332;FZ)V")
    private static void prominence$spellFrame(class_332 context,float delta,boolean preview,Operation<Void> original) {
        var previous=HudSpellReadScope.enter();
        try{original.call(context,delta,preview);}finally{HudSpellReadScope.leave(previous);}
    }
}
