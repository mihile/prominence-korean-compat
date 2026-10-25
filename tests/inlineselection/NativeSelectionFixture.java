package inlineselection;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
public final class NativeSelectionFixture {
    public static void main(String[] args)throws Exception {
        Path classes=Path.of(args[0]);int copied=0;
        // Execute the actual installed tab selection and cached-flag methods unchanged.
        try(var jar=new ZipFile(args[2])) {
            for(String type:List.of("StorageContainerMenuBase","UpgradeContainerBase")) {
                String name="net/p3pp3rf1y/sophisticatedcore/common/gui/"+type+".class";
                var original=new ClassNode();new ClassReader(jar.getInputStream(jar.getEntry(name)).readAllBytes()).accept(original,0);
                var fixture=new ClassNode();new ClassReader(Files.readAllBytes(classes.resolve(name))).accept(fixture,0);
                for(var method:original.methods)if(Set.of("setOpenTabId","getOpenContainer","isOpen","setIsOpen").contains(method.name)
                        ||method.name.startsWith("lambda$getOpenContainer$")||method.name.startsWith("lambda$setOpenTabId$")) {
                    fixture.methods.removeIf(m->m.name.equals(method.name)&&m.desc.equals(method.desc));fixture.methods.add(method);copied++;
                }
                var writer=new ClassWriter(0);fixture.accept(writer);Files.write(classes.resolve(name),writer.toByteArray());
            }
        }
        if(copied!=6)throw new AssertionError("Unexpected native method set: "+copied);
        try(var child=new URLClassLoader(new URL[]{classes.toUri().toURL(),Path.of(args[1]).toUri().toURL()},NativeSelectionFixture.class.getClassLoader()) {
            protected Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException {
                synchronized(getClassLoadingLock(name)) {
                    Class<?> c=findLoadedClass(name);
                    boolean own=name.startsWith("net.minecraft.")||name.startsWith("net.p3pp3rf1y.")||name.startsWith("local.prominence.backpackinline.")
                        ||name.startsWith("net.fabricmc.fabric.api.networking.v1.")||name.startsWith("elocindev.tierify.screen.")||name.equals("inlineselection.Probe")||name.equals("inlineselection.BaselineProbe");
                    if(c==null&&own)try{c=findClass(name);}catch(ClassNotFoundException ignored){}
                    if(c==null)c=super.loadClass(name,false);if(resolve)resolveClass(c);return c;
                }
            }
        }){child.loadClass("inlineselection."+(args.length>3&&args[3].equals("baseline")?"BaselineProbe":"Probe")).getMethod("main",String[].class).invoke(null,(Object)new String[0]);}
        System.out.println("PASS: six original Core selection/flag method bodies executed; no Minecraft/world/GL launched.");
    }
}
