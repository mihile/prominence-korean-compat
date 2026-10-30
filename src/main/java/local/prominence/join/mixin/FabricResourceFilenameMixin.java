package local.prominence.join.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.Locale;
import local.prominence.join.ResourceFilename;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets="net.fabricmc.fabric.impl.resource.loader.ModNioResourcePack", remap=false)
public abstract class FabricResourceFilenameMixin {
    @WrapOperation(method="getFilename", at=@At(value="INVOKE", target="Ljava/lang/String;format(Ljava/util/Locale;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;"), remap=false)
    private static String prominence$resourceFilename(Locale locale, String format, Object[] args, Operation<String> original) {
        String result=ResourceFilename.tryFormat(locale,format,args);
        return result!=null ? result : original.call(locale,format,args);
    }
}
