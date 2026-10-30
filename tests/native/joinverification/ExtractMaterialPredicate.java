package joinverification;
import java.nio.file.*;import java.util.*;import org.objectweb.asm.*;import org.objectweb.asm.tree.*;
/** Copy the actual installed private comparison method, without loading Minecraft/Iris. */
public final class ExtractMaterialPredicate {
 public static void main(String[] args)throws Exception{
  var node=new ClassNode();try(var in=ExtractMaterialPredicate.class.getClassLoader().getResourceAsStream("net/irisshaders/iris/shaderpack/materialmap/BlockMaterialMapping.class")){new ClassReader(in.readAllBytes()).accept(node,0);}
  var method=node.methods.stream().filter(m->m.name.equals("checkState")).findFirst().orElseThrow();method.access=Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC;method.name="check";
  var out=new ClassNode();out.version=Opcodes.V17;out.access=Opcodes.ACC_PUBLIC;out.name="local/prominence/join/NativeMaterialPredicate";out.superName="java/lang/Object";out.methods.add(method);
  var writer=new ClassWriter(0);out.accept(writer);Path target=Path.of(args[0]).resolve(out.name+".class");Files.createDirectories(target.getParent());Files.write(target,writer.toByteArray());
 }
}
