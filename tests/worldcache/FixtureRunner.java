package worldcachefixture;
import java.net.*;
import java.nio.file.*;
public final class FixtureRunner {
    public static void main(String[] args)throws Exception {
        try(var child=new URLClassLoader(new URL[]{Path.of(args[0]).toUri().toURL(),Path.of(args[1]).toUri().toURL()},FixtureRunner.class.getClassLoader()) {
            protected Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException {
                synchronized(getClassLoadingLock(name)) {
                    Class<?> found=findLoadedClass(name);
                    boolean own=name.startsWith("net.minecraft.")||name.startsWith("worldcachefixture.Probe")||name.startsWith("local.prominence.memory.WorldCache")||name.equals("local.prominence.memory.WorldReferencePruner")
                        ||name.startsWith("com.anthonyhilyard.iceberg.")||name.startsWith("dev.shadowsoffire.apotheosis.")||name.startsWith("aztech.modern_industrialization.items.armor.MIKeyMap")
                        ||name.startsWith("net.blay09.mods.balm.api.event.LivingFallEvent")||name.equals("net.fabricmc.fabric.impl.transfer.item.InventoryStorageImpl");
                    if(found==null&&own)try{found=findClass(name);}catch(ClassNotFoundException ignored){}
                    if(found==null)found=super.loadClass(name,false);if(resolve)resolveClass(found);return found;
                }
            }
        }){child.loadClass("worldcachefixture.Probe").getMethod("main",String[].class).invoke(null,(Object)new String[0]);}
    }
}
