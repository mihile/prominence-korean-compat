package workverification;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.util.*;
import javax.tools.ToolProvider;
import local.prominence.work.DeferredOpacityEntry;
import local.prominence.work.MapperLoadScope;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Runs the transformed native constructor against tiny block API doubles, not a game bootstrap. */
public final class OpacityExecutionFixture {
    public static void run(ClassNode transformed, Path root) throws Exception {
        Files.createDirectories(root);
        Map<String,String> sources=Map.of(
            "net/minecraft/class_1922.java","package net.minecraft; public interface class_1922 {}",
            "net/minecraft/class_2338.java","package net.minecraft; public class class_2338 {public static final class_2338 field_10980=new class_2338();}",
            "net/minecraft/class_2248.java","package net.minecraft; public class class_2248 {}",
            "net/minecraft/class_2397.java","package net.minecraft; public class class_2397 extends class_2248 {}",
            "net/minecraft/class_2680.java","package net.minecraft; public class class_2680 { public static int calls; public static boolean fail; private final class_2248 block; public class_2680(class_2248 block){this.block=block;} public class_2248 method_26204(){return block;} public int method_26193(class_1922 view,class_2338 pos){if(view==null||pos!=class_2338.field_10980)throw new AssertionError();calls++;if(fail)throw new IllegalStateException();return 11;} }",
            "me/cortex/voxy/common/world/other/Mapper.java","package me.cortex.voxy.common.world.other; import net.minecraft.*; public class Mapper {public static class StateEntry { public StateEntry(int id,class_2680 state){class_1922 view=new class_1922(){final class_2680 captured=state;};} } }");
        var args=new ArrayList<String>(List.of("--release","17","-d",root.toString()));
        for(var entry:sources.entrySet()) {Path p=root.resolve(entry.getKey());Files.createDirectories(p.getParent());Files.writeString(p,entry.getValue());args.add(p.toString());}
        if(ToolProvider.getSystemJavaCompiler().run(null,null,null,args.toArray(String[]::new))!=0)throw new AssertionError("API doubles compile failed");
        // Serialization is checked separately against original bytecode. Remove it only in this
        // executable fixture to avoid Minecraft registry/bootstrap dependencies.
        transformed.methods.removeIf(m->m.name.equals("serialize")||m.name.equals("deserialize"));
        ClassWriter writer=new ClassWriter(0);transformed.accept(writer);
        Files.write(root.resolve("me/cortex/voxy/common/world/other/Mapper$StateEntry.class"),writer.toByteArray());
        try(var loader=new URLClassLoader(new URL[]{root.toUri().toURL()},OpacityExecutionFixture.class.getClassLoader()) {
            @Override protected Class<?> loadClass(String n,boolean resolve)throws ClassNotFoundException {
                if(n.startsWith("net.minecraft.")||n.startsWith("me.cortex.voxy.common.world.other.Mapper")) {
                    synchronized(getClassLoadingLock(n)){Class<?> c=findLoadedClass(n);if(c==null)c=findClass(n);if(resolve)resolveClass(c);return c;}
                }
                return super.loadClass(n,resolve);
            }
        }) {
            Class<?> state=loader.loadClass("net.minecraft.class_2680"),block=loader.loadClass("net.minecraft.class_2248"),entry=loader.loadClass("me.cortex.voxy.common.world.other.Mapper$StateEntry");
            var stateCtor=state.getConstructor(block);var ctor=entry.getConstructor(int.class,state);var calls=state.getField("calls");
            Object ordinary=stateCtor.newInstance(block.getConstructor().newInstance());
            Object eager=ctor.newInstance(1,ordinary);
            if(calls.getInt(null)!=1||entry.getField("opacity").getInt(eager)!=11)throw new AssertionError("new entry changed");
            Object lazy,leaf;
            try(var scope=MapperLoadScope.begin()) {
                lazy=ctor.newInstance(2,ordinary);
                leaf=ctor.newInstance(3,stateCtor.newInstance(loader.loadClass("net.minecraft.class_2397").getConstructor().newInstance()));
                if(calls.getInt(null)!=1)throw new AssertionError("saved mapping eager");
            }
            if(((DeferredOpacityEntry)leaf).prominence$resolvedOpacity()!=15||calls.getInt(null)!=1)throw new AssertionError("leaves changed");
            state.getField("fail").setBoolean(null,true);
            try {((DeferredOpacityEntry)lazy).prominence$resolvedOpacity();throw new AssertionError("native error swallowed");}catch(IllegalStateException expected){}
            state.getField("fail").setBoolean(null,false);
            if(((DeferredOpacityEntry)lazy).prominence$resolvedOpacity()!=11||entry.getField("opacity").getInt(lazy)!=11)throw new AssertionError("native result mismatch");
            ((DeferredOpacityEntry)lazy).prominence$resolvedOpacity();
            if(calls.getInt(null)!=3)throw new AssertionError("native operation called repeatedly");
            var pending=Arrays.stream(entry.getDeclaredFields()).filter(f->f.getName().contains("pendingOpacity")).findFirst().orElseThrow();
            pending.setAccessible(true);
            if(pending.get(lazy)!=null)throw new AssertionError("completed entry retained helper");
        }
        System.out.println("PASS: transformed StateEntry executed with block API doubles: eager new entries, deferred loaded entries, leaves15, native failure/retry, exact result and once-only operation");
    }
}
