package local.prominence.memory.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.class_1937;
import org.spongepowered.asm.mixin.*;
import xaero.common.minimap.MinimapProcessor;
import xaero.common.minimap.write.biome.BiomeBlendCalculator;

@Mixin(targets="xaero.common.minimap.write.MinimapWriter",remap=false)
public abstract class XaeroBiomeWriterMixin {
    @Shadow @Final private BiomeBlendCalculator biomeBlendCalculator;
    @WrapMethod(method="writeChunk(Lxaero/common/minimap/MinimapProcessor;DDDLnet/minecraft/class_1937;IZ)Z",remap=false)
    private boolean prominence$worldDuringWrite(MinimapProcessor processor,double x,double y,double z,class_1937 world,int caving,boolean flag,Operation<Boolean> original){
        var access=(XaeroBiomeWorldAccess)(Object)biomeBlendCalculator;
        class_1937 previous=access.prominence$world();access.prominence$world(world);
        try{return original.call(processor,x,y,z,world,caving,flag);}
        finally{access.prominence$world(previous);}
    }
}
