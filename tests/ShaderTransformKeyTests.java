package local.prominence.memory;

import com.google.gson.*;
import it.unimi.dsi.fastutil.objects.*;
import java.nio.file.*;
import java.util.*;
import net.irisshaders.iris.gl.blending.*;
import net.irisshaders.iris.gl.state.ShaderAttributeInputs;
import net.irisshaders.iris.gl.texture.TextureType;
import net.irisshaders.iris.helpers.Tri;
import net.irisshaders.iris.pipeline.transform.*;
import net.irisshaders.iris.pipeline.transform.parameter.*;
import net.irisshaders.iris.shaderpack.texture.TextureStage;

public final class ShaderTransformKeyTests {
    private static int checks;
    private static void check(boolean ok, String message) { checks++; if (!ok) throw new AssertionError(message); }
    private static Parameters vanilla(Object2ObjectMap<Tri<String,TextureType,TextureStage>,String> map, boolean lines, boolean offset, boolean geometry, boolean tess, float alpha) {
        return new VanillaParameters(Patch.VANILLA,map,new AlphaTest(AlphaTestFunction.GREATER,alpha),lines,offset,
            new ShaderAttributeInputs(true,true,true,true,true),geometry,tess);
    }
    public static void main(String[] args) throws Exception {
        var map = new Object2ObjectOpenHashMap<Tri<String,TextureType,TextureStage>,String>();
        var texture = new Tri<>("source",TextureType.TEXTURE_2D,TextureStage.GBUFFERS_AND_SHADOW); map.put(texture,"renamed");
        var input = new ShaderAttributeInputs(true,true,true,true,true);
        var examples = List.of(vanilla(map,false,false,false,false,.1f),new SodiumParameters(Patch.SODIUM,map,AlphaTest.ALWAYS,input),
            new TextureStageParameters(Patch.COMPOSITE,TextureStage.COMPOSITE_AND_FINAL,map),
            new ComputeParameters(Patch.COMPUTE,TextureStage.COMPOSITE_AND_FINAL,map),new DHParameters(Patch.DH_GENERIC,map));
        for (var original:examples) {
            original.type=null; original.name="before";
            var key=ShaderTransformKey.snapshot(original); int hash=key.hashCode();
            check(key!=original && key.getClass()==original.getClass() && key.equals(original),"Snapshot lost native inputs");
            check(key.getTextureMap()!=map && key.getTextureMap().equals(map),"Texture mapping was not isolated");
            for (var stage:PatchShaderType.values()) {
                original.type=stage; original.name="changed";
                check(key.hashCode()==hash && key.type==null,"Job mutation changed stored key");
                original.type=null;
                check(key.equals(original),"Equivalent fresh job misses snapshot");
            }
            map.put(texture,"other"); check(key.hashCode()==hash && !key.equals(original),"Texture edits reused stale output");map.put(texture,"renamed");
            check(key.equals(original),"Restored mapping differs");
        }
        var base=ShaderTransformKey.snapshot(vanilla(map,false,false,false,false,.1f));
        for(int bits=0;bits<16;bits++) for(float alpha:new float[]{.1f,.2f}) {
            var changed=vanilla(map,(bits&1)!=0,(bits&2)!=0,(bits&4)!=0,(bits&8)!=0,alpha);
            // Native Parameters omits tessellation from equality; native CacheKey also compares
            // the tessellation source strings. Preserve that contract, and copy the flag exactly.
            check(base.equals(changed)==((bits&7)==0&&alpha==.1f),"Native geometry/lines/offset/alpha contract changed");
            check(((VanillaParameters)ShaderTransformKey.snapshot(changed)).hasTesselation==((bits&8)!=0),"Tessellation flag lost");
        }
        check(ShaderTransformKey.snapshot(null)==null,"Null fallback changed");
        Parameters unknown=new DHParameters(Patch.DH_GENERIC,map){};
        check(ShaderTransformKey.snapshot(unknown)==unknown,"Unknown subclass was copied");
        var customMap=new Object2ObjectOpenHashMap<Tri<String,TextureType,TextureStage>,String>(){};
        var custom=vanilla(customMap,false,false,false,false,.1f);
        check(ShaderTransformKey.snapshot(custom)==custom,"Custom map semantics changed");
        var nullMap=vanilla(null,false,false,false,false,.1f);check(ShaderTransformKey.snapshot(nullMap).equals(nullMap),"Null texture map changed");
        var pins=JsonParser.parseString(Files.readString(Path.of(args[0]))).getAsJsonObject();
        java.util.function.Function<String,byte[]> read=p->{try(var in=ShaderTransformKeyTests.class.getClassLoader().getResourceAsStream(p)){return in==null?null:in.readAllBytes();}catch(Exception e){return null;}};
        check(ShaderTransformGuard.matches(pins,"1.20.1","1.7.6+mc1.20.1",read),"Installed native guard rejected");
        check(!ShaderTransformGuard.matches(pins,"1.21.1","1.7.6+mc1.20.1",read),"Wrong game accepted");
        check(!ShaderTransformGuard.matches(pins,"1.20.1","different",read),"Wrong Iris accepted");
        check(!ShaderTransformGuard.matches(pins,"1.20.1","1.7.6+mc1.20.1",p->null),"Missing class accepted");
        check(!ShaderTransformGuard.matches(pins,"1.20.1","1.7.6+mc1.20.1",p->new byte[]{1}),"Modified class accepted");
        System.setProperty("prominence.shaderTransformKey","false");
        try {var guard=new ShaderTransformGuard();guard.onLoad("");check(!guard.shouldApplyMixin("",""),"Opt-out ignored");}
        finally {System.clearProperty("prominence.shaderTransformKey");}
        System.out.println("PASS: "+checks+" native parameter isolation/input distinction/fallback/guard checks; no game/GL");
    }
}
