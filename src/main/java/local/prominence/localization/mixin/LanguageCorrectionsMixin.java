package local.prominence.localization.mixin;

import com.google.common.collect.ImmutableMap;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import java.util.*;
import local.prominence.localization.TranslationOverrides;
import net.minecraft.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(class_1078.class)
public abstract class LanguageCorrectionsMixin {
    // Apply after every native pack/language entry has been read, before the immutable snapshot.
    @WrapOperation(method="method_4675",at=@At(value="INVOKE",target="Lcom/google/common/collect/ImmutableMap;copyOf(Ljava/util/Map;)Lcom/google/common/collect/ImmutableMap;"),require=1)
    private static ImmutableMap<String,String> prominence$reviewedLanguage(Map<String,String> loaded,Operation<ImmutableMap<String,String>> original,class_3300 resources,List<String> languages,boolean rightToLeft){
        return original.call(TranslationOverrides.apply(loaded,languages));
    }
}
