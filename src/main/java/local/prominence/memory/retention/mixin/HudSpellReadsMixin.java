package local.prominence.memory.retention.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import local.prominence.memory.HudSpellReadScope;
import net.minecraft.*;
import net.spell_engine.api.spell.SpellContainer;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(targets="net.spell_engine.internals.SpellContainerHelper",remap=false)
public abstract class HudSpellReadsMixin {
    @WrapMethod(method="getEquipped(Lnet/minecraft/class_1799;Lnet/minecraft/class_1657;)Lnet/spell_engine/api/spell/SpellContainer;")
    private static SpellContainer prominence$readEquipped(class_1799 stack,class_1657 player,Operation<SpellContainer> original) {
        return HudSpellReadScope.equipped(stack,player,original);
    }
}
