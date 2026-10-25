package xaerobiome;
import java.nio.file.*;import java.net.*;
public final class ScopeRunner {
    public static void main(String[]args)throws Exception {
        try(var child=new URLClassLoader(new URL[]{Path.of(args[0]).toUri().toURL(),Path.of(args[1]).toUri().toURL(),Path.of(args[2]).toUri().toURL()},ScopeRunner.class.getClassLoader()){
            protected Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException {
                synchronized(getClassLoadingLock(name)){
                    Class<?> c=findLoadedClass(name);boolean own=name.equals("net.minecraft.class_1937")||name.startsWith("xaerobiome.ScopeProbe")||name.equals("local.prominence.memory.mixin.XaeroBiomeWorldAccess")||name.equals("local.prominence.memory.mixin.XaeroBiomeWriterMixin")||name.equals("xaero.common.minimap.write.biome.BiomeBlendCalculator");
                    if(c==null&&own)try{c=findClass(name);}catch(ClassNotFoundException ignored){}
                    if(c==null)c=super.loadClass(name,false);if(resolve)resolveClass(c);return c;
                }
            }
        }){child.loadClass("xaerobiome.ScopeProbe").getMethod("main",String[].class).invoke(null,(Object)new String[0]);}
    }
}
