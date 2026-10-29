import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipFile;
import com.google.gson.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

public final class PerformanceRetirementChecks {
    public static void main(String[] args)throws Exception {
        Set<String> forbidden=Set.of("IrisPreprocessDiskCacheMixin","VoxyMeshServiceMixin","VoxyMeshRunMixin","UnchangedStateRemapMixin");
        Set<String> active=new HashSet<>();
        try(var jar=new ZipFile(args[0])){
            var meta=JsonParser.parseString(new String(jar.getInputStream(jar.getEntry("fabric.mod.json")).readAllBytes(),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
            for(var value:meta.getAsJsonArray("mixins")){
                String config=value.isJsonPrimitive()?value.getAsString():value.getAsJsonObject().get("config").getAsString();
                var node=JsonParser.parseString(new String(jar.getInputStream(jar.getEntry(config)).readAllBytes(),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
                for(String key:List.of("client","mixins"))if(node.has(key))for(var name:node.getAsJsonArray(key))active.add(name.getAsString());
            }
        }
        for(String retired:forbidden)if(active.contains(retired))throw new AssertionError("Retired hook still registered: "+retired);
        for(String kept:List.of("IrisPipelineCacheLifetimeMixin","VoxyModelWorkSliceMixin","EmiCraftableRefreshMixin","LootConditionsVisibleMixin","BackpackStonecuttingMixin"))if(!active.contains(kept))throw new AssertionError("Unrelated fix lost: "+kept);
        Path root=Path.of(args[1]);var timing=read(root,"memory/mixin/IrisPipelineTimingMixin");
        for(var method:timing.methods)for(var i:method.instructions)if(i instanceof MethodInsnNode call && call.owner.endsWith("DimensionChangeGate"))throw new AssertionError("Stack-based destruction suppression remains");
        var bridge=read(root,"memory/ShaderPreprocessBridge");var report=bridge.methods.stream().filter(m->m.name.equals("report")).findFirst().orElseThrow();
        for(var i:report.instructions){
            if(i instanceof FieldInsnNode field && field.owner.contains("Holder"))throw new AssertionError("Timing starts retired cache");
            if(i instanceof MethodInsnNode call && call.owner.contains("ShaderSourceDiskCache"))throw new AssertionError("Timing performs cache work");
        }
        System.out.println("PASS retired cache/mesh/remap hooks absent, native lifetime routing, no cache in timer; memory/gameplay/backpack fixes retained");
    }
    static ClassNode read(Path root,String name)throws Exception{var node=new ClassNode();new ClassReader(Files.readAllBytes(root.resolve("local/prominence/"+name+".class"))).accept(node,0);return node;}
}
