package local.prominence.work.mixin;
import local.prominence.work.LootUiText;import net.minecraft.*;import fzzyhmstrs.emi_loot.EMILoot;import org.spongepowered.asm.mixin.Mixin;import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
@Mixin(targets="fzzyhmstrs.emi_loot.util.stack.EmiStackWithTitle",remap=false) public abstract class LootTitleSharedMixin {
 @WrapMethod(method="getRawTitle") private class_5250 title(class_2960 id,String namespace,String path,EMILoot.Type type,Operation<class_5250> original){if(namespace.equals("chest")){var t=LootUiText.title(id);if(t!=null)return t;}return original.call(id,namespace,path,type);}
}
