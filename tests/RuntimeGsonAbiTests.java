import java.nio.file.Path;
import java.util.*;
import java.util.zip.ZipFile;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Link every Gson member referenced by the cumulative JAR against the launcher's Gson. */
public final class RuntimeGsonAbiTests {
    private static boolean method(Class<?> owner, String name, String descriptor, Set<Class<?>> visited) {
        if (owner == null || !visited.add(owner)) return false;
        if (name.equals("<init>")) {
            for (var c : owner.getDeclaredConstructors()) if (Type.getConstructorDescriptor(c).equals(descriptor)) return true;
            return false;
        }
        for (var m : owner.getDeclaredMethods()) if (m.getName().equals(name) && Type.getMethodDescriptor(m).equals(descriptor)) return true;
        if (method(owner.getSuperclass(),name,descriptor,visited)) return true;
        for (var i : owner.getInterfaces()) if (method(i,name,descriptor,visited)) return true;
        return false;
    }
    private static boolean field(Class<?> owner, String name, String descriptor) {
        if (owner == null) return false;
        for (var f : owner.getDeclaredFields()) if (f.getName().equals(name) && Type.getDescriptor(f.getType()).equals(descriptor)) return true;
        if (field(owner.getSuperclass(),name,descriptor)) return true;
        for (var i : owner.getInterfaces()) if (field(i,name,descriptor)) return true;
        return false;
    }
    public static void main(String[] args) throws Exception {
        ClassLoader loader=RuntimeGsonAbiTests.class.getClassLoader();
        var checked=new HashSet<String>(); int classes=0;
        try (var zip=new ZipFile(Path.of(args[0]).toFile())) {
            var entries=zip.entries();
            while(entries.hasMoreElements()) {
                var entry=entries.nextElement(); if(!entry.getName().endsWith(".class"))continue; classes++;
                var node=new ClassNode();try(var in=zip.getInputStream(entry)){new ClassReader(in).accept(node,ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);}
                for(var m:node.methods)for(var instruction:m.instructions) {
                    if(instruction instanceof MethodInsnNode call && call.owner.startsWith("com/google/gson/")) {
                        String key=call.owner+"."+call.name+call.desc;
                        if(checked.add(key)&&!method(Class.forName(call.owner.replace('/','.'),false,loader),call.name,call.desc,new HashSet<>()))
                            throw new AssertionError("Gson method is absent at runtime: "+key+" from "+node.name+"."+m.name);
                    } else if(instruction instanceof FieldInsnNode f && f.owner.startsWith("com/google/gson/")) {
                        String key=f.owner+"."+f.name+f.desc;
                        if(checked.add(key)&&!field(Class.forName(f.owner.replace('/','.'),false,loader),f.name,f.desc))
                            throw new AssertionError("Gson field is absent at runtime: "+key);
                    }
                }
            }
        }
        System.out.println("PASS: "+classes+" cumulative classes / "+checked.size()+" Gson member links; runtime="+
            com.google.gson.JsonObject.class.getProtectionDomain().getCodeSource().getLocation());
    }
}
