package local.prominence.memory;

import com.github.nyuppo.variant.MobVariant;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.*;
import net.minecraft.class_2487;
import net.minecraft.class_2960;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;

/** Native NBT/variant values and fingerprints; inert owner fixtures only, no game/world startup. */
public final class BatchMemoryTests {
    private static int checks;
    private static void check(boolean value,String text){checks++;if(!value)throw new AssertionError(text);}
    private static class Holder {private MobVariant variant;Object world;Holder(MobVariant v){variant=v;}}
    private static final class Derived extends Holder {Derived(MobVariant v){super(v);}}
    private static final class DoubleOwner {double value;DoubleOwner(double value){this.value=value;}}
    private static double evaluate(DoubleOwner o){return o.value;}
    private static WeakDoubleSupplier<DoubleOwner> weakSupplier(){return new WeakDoubleSupplier<>(new DoubleOwner(9),BatchMemoryTests::evaluate);}
    private static WeakReference<Object> projectTemporary(class_2487 nbt) {
        Object world=new Object();Holder holder=new Holder(new MobVariant(new class_2960("moremobvariants","cow"),1));holder.world=world;
        check(VariantProjection.project(holder,nbt),"Temporary projection failed");return new WeakReference<>(world);
    }
    private static WeakReference<AzureRendererOwner> trackTemporary() {
        AzureRendererOwner renderer=new AzureRendererOwner(){Object world=new Object();public boolean prominence$clearObsoleteWorld(Object current){world=null;return true;}};
        AzureRendererTracker.track(renderer);return new WeakReference<>(renderer);
    }
    private static byte[] raw(String name)throws Exception {
        try(var in=BatchMemoryTests.class.getClassLoader().getResourceAsStream(name)){if(in==null)throw new AssertionError("Missing native "+name);return in.readAllBytes();}
    }
    public static void main(String[] args)throws Exception {
        MobVariant one=new MobVariant(new class_2960("moremobvariants","default"),1),two=new MobVariant(new class_2960("moremobvariants","speckled"),2);
        Holder holder=new Holder(one);class_2487 nbt=new class_2487();nbt.method_10582("unrelated","keep");
        check(VariantProjection.project(holder,nbt),"Native variant field unavailable");
        check(nbt.method_10558("VariantID").equals(one.getIdentifier().toString()),"Wrong native variant ID");
        check(nbt.method_10558("unrelated").equals("keep"),"Caller NBT discarded");
        holder.variant=two;check(VariantProjection.project(holder,nbt)&&nbt.method_10558("VariantID").equals(two.getIdentifier().toString()),"Variant update stale-cached");
        Derived subclass=new Derived(two);check(VariantProjection.project(subclass,nbt),"Inherited native variant ignored");
        holder.variant=null;check(!VariantProjection.project(holder,nbt),"Null variant should use native fallback");
        check(!VariantProjection.project(new Object(),nbt)&&!VariantProjection.project(null,nbt),"Unknown entity must use native serialization");
        Object missing=new Object();for(int i=0;i<20;i++)check(!VariantProjection.project(missing,nbt),"Missing-field fallback changed");
        var world=projectTemporary(nbt);for(int i=0;i<30&&world.get()!=null;i++){System.gc();Thread.sleep(20);}
        check(world.get()==null,"ClassValue or NBT retained the projected owner/world");Reference.reachabilityFence(nbt);
        DoubleOwner active=new DoubleOwner(3.5);var supplier=new WeakDoubleSupplier<>(active,BatchMemoryTests::evaluate);
        check(supplier.getAsDouble()==3.5,"Current animation value changed");active.value=6.25;check(supplier.getAsDouble()==6.25,"Animation value not live");
        var expired=weakSupplier();for(int i=0;i<30&&expired.get()!=null;i++){System.gc();Thread.sleep(20);}
        check(expired.get()==null&&expired.getAsDouble()==0,"Animation callback retains old owner");Reference.reachabilityFence(expired);Reference.reachabilityFence(active);
        var renderer=trackTemporary();for(int i=0;i<30&&renderer.get()!=null;i++){System.gc();Thread.sleep(20);}
        check(renderer.get()==null,"Renderer registry is strong");AzureRendererTracker.release(null);
        int[] released={0};AzureRendererOwner live=current->{released[0]++;return true;};AzureRendererTracker.track(live);AzureRendererTracker.track(live);
        check(AzureRendererTracker.release(new Object())==1&&released[0]==1,"Duplicate renderer registration");Reference.reachabilityFence(live);

        var pins=BatchMemoryGuard.pins();check(pins.size()==6,"Incomplete batch guards");
        for(String module:pins.keySet()) {
            Map<String,String> versions=new HashMap<>();Map<String,byte[]> bytes=new HashMap<>();
            for(var dep:pins.getAsJsonObject(module).getAsJsonArray("dependencies")) {
                var d=dep.getAsJsonObject();versions.put(d.get("mod").getAsString(),d.get("version").getAsString());
                for(String path:d.getAsJsonObject("classes").keySet())bytes.put(path,raw(path));
            }
            check(BatchMemoryGuard.matches(module,versions,bytes),"Installed fingerprints rejected: "+module);
            check(!BatchMemoryGuard.matches(module,Map.of(),bytes),"Missing dependency version accepted");
            for(String path:List.copyOf(bytes.keySet())) {
                byte[] original=bytes.get(path),changed=original.clone();changed[changed.length/2]^=1;
                bytes.put(path,changed);check(!BatchMemoryGuard.matches(module,versions,bytes),"Altered native accepted: "+path);
                bytes.remove(path);check(!BatchMemoryGuard.matches(module,versions,bytes),"Missing native accepted: "+path);
                bytes.put(path,original);check(BatchMemoryGuard.matches(module,versions,bytes),"Restored native rejected");
            }
        }
        check(!BatchMemoryGuard.matches("unknown",Map.of(),Map.of()),"Unknown module accepted");
        // Ensure live modifiers use the native effective identifier, not the raw base ID.
        ClassNode projection=new ClassNode();new ClassReader(raw("local/prominence/memory/VariantProjection.class")).accept(projection,0);
        boolean effective=false,rawId=false;for(var m:projection.methods)for(var i:m.instructions)if(i instanceof MethodInsnNode call&&call.owner.equals("com/github/nyuppo/variant/MobVariant")){effective|=call.name.equals("getIdentifier");rawId|=call.name.equals("getRawIdentifier");}
        check(effective&&!rawId,"Custom variant name semantics bypassed");
        System.out.println("PASS: "+checks+" batch native fingerprints, live NBT projection/fallback, weak animation/world/renderer lifetime checks; no Minecraft world");
    }
}
