package xaerobiome;
import java.lang.ref.*;import java.lang.reflect.*;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import local.prominence.memory.mixin.*;
import net.minecraft.class_1937;
import xaero.common.minimap.write.biome.BiomeBlendCalculator;
public final class ScopeProbe extends XaeroBiomeWriterMixin {
    static int checks;static Method wrapper;static ScopeProbe holder;static BiomeBlendCalculator calculator;static XaeroBiomeWorldAccess access;
    static void check(boolean b,String m){checks++;if(!b)throw new AssertionError(m);}
    static int bound(Field f){try{return f.getInt(calculator);}catch(IllegalAccessException e){throw new AssertionError(e);}}
    static boolean run(class_1937 world,Operation<Boolean> operation)throws Throwable {
        try{return (boolean)wrapper.invoke(holder,null,1.25,2.5,3.75,world,4,true,operation);}catch(InvocationTargetException e){throw e.getCause();}
    }
    static WeakReference<class_1937> oldOwner()throws Throwable {
        var world=new class_1937(77);var weak=new WeakReference<>(world);
        run(world,a->{check(access.prominence$world()==world,"Owner valid during native callback");return true;});return weak;
    }
    public static void main(String[]args)throws Throwable {
        holder=new ScopeProbe();calculator=new BiomeBlendCalculator();access=(XaeroBiomeWorldAccess)(Object)calculator;
        var field=XaeroBiomeWriterMixin.class.getDeclaredField("biomeBlendCalculator");field.setAccessible(true);field.set(holder,calculator);
        wrapper=java.util.Arrays.stream(XaeroBiomeWriterMixin.class.getDeclaredMethods()).filter(m->m.getName().equals("prominence$worldDuringWrite")).findFirst().orElseThrow();wrapper.setAccessible(true);
        var a=new class_1937(11);var b=new class_1937(22);int[] calls={0};
        check(run(a,v->{calls[0]++;check(v.length==7&&v[0]==null&&(double)v[1]==1.25&&(double)v[2]==2.5&&(double)v[3]==3.75&&v[4]==a&&(int)v[5]==4&&(boolean)v[6],"Exact native arguments");calculator.prepare(a,true);check(calculator.method_31605()==11,"Real native calculator delegates during job");return true;}),"Return true preserved");
        check(calls[0]==1,"Native job executes once");check(access.prominence$world()==null,"Owner released after job");
        var bounds=BiomeBlendCalculator.class.getDeclaredField("startI");bounds.setAccessible(true);check(bounds.getInt(calculator)==-1,"Blending bounds remain after release");
        check(!run(b,v->{check(calculator.method_31605()==22,"Incremental call without prepare binds current world");check(bound(bounds)==-1,"Incremental call preserves native blend settings");return false;}),"Return false preserved");
        check(access.prominence$world()==null,"Second job releases owner");
        var failure=new IllegalStateException("native failure");try{run(a,v->{throw failure;});throw new AssertionError("Exception swallowed");}catch(IllegalStateException e){check(e==failure,"Exception identity preserved");}
        check(access.prominence$world()==null,"Exception releases owner");
        run(a,v->{try{run(b,w->{check(access.prominence$world()==b,"Nested owner installed");return true;});}catch(Throwable e){throw new AssertionError(e);}check(access.prominence$world()==a,"Nested owner restores outer call");return true;});
        check(access.prominence$world()==null,"Outer owner released after nested call");
        access.prominence$world(a);run(b,v->true);check(access.prominence$world()==a,"Preexisting active scope preserved");access.prominence$world(null);
        run(null,v->{check(access.prominence$world()==null,"Native null context unchanged");return true;});
        var weak=oldOwner();for(int i=0;i<20&&weak.get()!=null;i++){System.gc();Thread.sleep(10);}
        check(weak.get()==null,"Previous data-only world collects while writer/calculator remain alive");
        check(access.prominence$world()==null,"No persistent owner after collection");
        System.out.println("PASS: "+checks+" production Xaero scope checks; actual calculator prepare/delegation, incremental jobs, nested calls, exceptions, unchanged bounds, old owner collection; no Minecraft world/GL.");
    }
}
