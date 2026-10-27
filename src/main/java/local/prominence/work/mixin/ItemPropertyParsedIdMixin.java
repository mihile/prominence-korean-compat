package local.prominence.work.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import local.prominence.work.ParsedItemId;
import net.minecraft.class_2960;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets="elocindev.customitemattributes.api.ItemProperty",remap=false)
public abstract class ItemPropertyParsedIdMixin {
    // Gson creates these configuration objects without invoking their constructors.
    @Unique private transient volatile ParsedItemId prominence$parsedItemId;
    @WrapOperation(method="getItem",at=@At(value="NEW",target="(Ljava/lang/String;)Lnet/minecraft/class_2960;"),remap=false)
    private class_2960 prominence$reuseParsedId(String text,Operation<class_2960> original) {
        ParsedItemId memo=prominence$parsedItemId;
        if(memo==null)prominence$parsedItemId=memo=new ParsedItemId();
        return memo.resolve(text,original);
    }
}
