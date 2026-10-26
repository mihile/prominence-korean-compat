package local.prominence.swingscope.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import local.prominence.swingscope.SwingScope;
import net.minecraft.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value=class_1309.class,priority=500)
public abstract class AnimationSwingMixin {
    // Includes native injected callbacks without relying on generated handler names.
    @WrapMethod(method="method_23667(Lnet/minecraft/class_1268;Z)V",require=1)
    private void prominence$animationScope(class_1268 hand,boolean fromServerPlayer,Operation<Void> original){
        SwingScope.enterAnimation();
        try{original.call(hand,fromServerPlayer);}finally{SwingScope.exitAnimation();}
    }
}
