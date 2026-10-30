package local.prominence.memory;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.irisshaders.iris.gl.state.ShaderAttributeInputs;
import net.irisshaders.iris.gl.texture.TextureType;
import net.irisshaders.iris.helpers.Tri;
import net.irisshaders.iris.pipeline.transform.parameter.*;
import net.irisshaders.iris.shaderpack.texture.TextureStage;

/** The native transform cache must not retain the mutable parameters used by the AST job. */
public final class ShaderTransformKey {
    private ShaderTransformKey() {}

    public static Parameters snapshot(Parameters source) {
        if (source == null) return null;
        Class<?> kind = source.getClass();
        if (kind != VanillaParameters.class && kind != SodiumParameters.class
                && kind != TextureStageParameters.class && kind != ComputeParameters.class
                && kind != DHParameters.class) return source;
        ShaderAttributeInputs inputs = source instanceof VanillaParameters v ? v.inputs
            : source instanceof SodiumParameters s ? s.inputs : null;
        if (inputs != null && inputs.getClass() != ShaderAttributeInputs.class) return source;
        var textures = source.getTextureMap();
        Object2ObjectMap<Tri<String, TextureType, TextureStage>, String> copy = null;
        if (textures != null) {
            // Only copy the native data maps; custom maps keep their own lookup/iteration semantics.
            String mapClass = textures.getClass().getName();
            if (!mapClass.equals("it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap")
                    && !mapClass.equals("it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap")
                    && !mapClass.equals("it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap")
                    && !mapClass.equals("it.unimi.dsi.fastutil.objects.Object2ObjectMaps$EmptyMap")
                    && !mapClass.equals("it.unimi.dsi.fastutil.objects.Object2ObjectMaps$Singleton")) return source;
            copy = new Object2ObjectOpenHashMap<>(textures.size());
            for (var entry : textures.object2ObjectEntrySet()) {
                Object rawKey = entry.getKey(), rawValue = entry.getValue();
                if (!(rawKey instanceof Tri<?, ?, ?> key) || !(key.first() instanceof String)
                        || !(key.second() instanceof TextureType) || !(key.third() instanceof TextureStage)
                        || !(rawValue instanceof String)) return source;
                copy.put(entry.getKey(), entry.getValue());
            }
        }
        Parameters result;
        if (source instanceof VanillaParameters v) {
            result = new VanillaParameters(v.patch, copy, v.alpha, v.isLines(), v.hasChunkOffset,
                v.inputs, v.hasGeometry, v.hasTesselation);
        } else if (source instanceof SodiumParameters s) {
            result = new SodiumParameters(s.patch, copy, s.alpha, s.inputs);
        } else if (kind == ComputeParameters.class) {
            result = new ComputeParameters(source.patch, source.getTextureStage(), copy);
        } else if (kind == TextureStageParameters.class) {
            result = new TextureStageParameters(source.patch, source.getTextureStage(), copy);
        } else {
            result = new DHParameters(source.patch, copy);
        }
        result.type = source.type;
        result.name = source.name;
        return result;
    }
}
