package local.prominence.weaponhud.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import java.util.List;
import net.minecraft.class_315;
import net.minecraft.class_746;
import net.spell_engine.client.input.SpellHotbar;
import net.spell_engine.client.input.WrappedKeybinding;
import net.spell_engine.mixin.client.control.KeybindingAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Reserve item use before assigning spells so input and HUD use the same compact order. */
@Mixin(value=SpellHotbar.class,remap=false)
public abstract class SpellBindingMixin {
    @ModifyExpressionValue(method="update",at=@At(value="INVOKE",target="Lnet/spell_engine/client/input/Keybindings$Wrapped;all()Ljava/util/List;"))
    private List<WrappedKeybinding> prominence$reserveItemUse(List<WrappedKeybinding> bindings,class_746 player,class_315 options){
        var use=((KeybindingAccessor)options.field_1904).getBoundKey();
        return bindings.stream().filter(binding->{
            var resolved=binding.get(options);
            if(resolved==null)return true;
            var key=((KeybindingAccessor)resolved.keyBinding()).getBoundKey();
            return !use.equals(key)&&!"key.mouse.right".equals(key.method_1441());
        }).toList();
    }
}
