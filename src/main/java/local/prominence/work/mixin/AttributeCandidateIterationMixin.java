package local.prominence.work.mixin;

import com.google.common.collect.Multimap;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import elocindev.customitemattributes.api.ItemProperty;
import java.util.*;
import local.prominence.work.AttributeCandidates;
import net.minecraft.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets="elocindev.customitemattributes.builder.AttributeBuilderCallback",remap=false)
public abstract class AttributeCandidateIterationMixin {
    @WrapOperation(method="lambda$register$0",at=@At(value="INVOKE",target="Ljava/util/List;iterator()Ljava/util/Iterator;",ordinal=0),remap=false)
    private static Iterator<ItemProperty> prominence$matchingDefinitions(List<ItemProperty> source,
        Operation<Iterator<ItemProperty>> original,class_1799 stack,class_1304 slot,Multimap<?,?> modifiers){
        if(!AttributeCandidates.active()||stack==null||stack.getClass()!=class_1799.class||source==null||source.getClass()!=ArrayList.class||source.isEmpty())return original.call(source);
        // Missing IDs resolve to AIR in the defaulted registry. Keep that complete
        // native path, and unknown/unregistered items, rather than prefiltering it.
        var selected=AttributeCandidates.select(source,()->{
            var item=stack.method_7909();var id=class_7923.field_41178.method_10221(item);
            return id==null||id.toString().equals("minecraft:air")||class_7923.field_41178.method_10223(id)!=item?null:id;
        });
        return selected==null?original.call(source):selected.iterator();
    }
}
