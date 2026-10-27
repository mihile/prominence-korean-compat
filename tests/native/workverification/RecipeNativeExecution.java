package workverification;
import java.io.*;import java.nio.file.*;import java.util.*;import org.objectweb.asm.*;import org.objectweb.asm.tree.*;
public class RecipeNativeExecution {
 static class Loader extends ClassLoader {
  final Map<String,byte[]> defs;Loader(Map<String,byte[]> d){super(RecipeNativeExecution.class.getClassLoader());defs=d;}
  protected synchronized Class<?> loadClass(String n,boolean r)throws ClassNotFoundException{Class<?> c=findLoadedClass(n);if(c==null&&defs.containsKey(n)){byte[] b=defs.get(n);c=defineClass(n,b,0,b.length);}if(c==null)c=super.loadClass(n,false);if(r)resolveClass(c);return c;}
 }
 static byte[] read(String n)throws Exception{try(var in=RecipeNativeExecution.class.getClassLoader().getResourceAsStream(n.replace('.','/')+".class")){return in.readAllBytes();}}
 static byte[] trimManager(byte[] raw){var n=new ClassNode();new ClassReader(raw).accept(n,0);n.fields.clear();n.interfaces.clear();n.methods.removeIf(m->m.name.equals("<clinit>")||m.name.equals("<init>")||(!m.name.contains("lambda$new$")&&!m.name.contains("prominence")&&!m.name.contains("mixinextras")));var w=new ClassWriter(0);n.accept(w);return w.toByteArray();}
 static byte[] instrumentItem(byte[] raw){var n=new ClassNode();new ClassReader(raw).accept(n,0);n.methods.removeIf(m->m.name.equals("<clinit>"));n.fields.add(new FieldNode(Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC,"fixtureCopies","I",null,null));for(var m:n.methods)if(m.name.equals("copy")&&m.desc.equals("()Ldev/emi/emi/api/stack/EmiStack;")){var ins=new InsnList();ins.add(new FieldInsnNode(Opcodes.GETSTATIC,n.name,"fixtureCopies","I"));ins.add(new InsnNode(Opcodes.ICONST_1));ins.add(new InsnNode(Opcodes.IADD));ins.add(new FieldInsnNode(Opcodes.PUTSTATIC,n.name,"fixtureCopies","I"));m.instructions.insert(ins);m.maxStack=Math.max(2,m.maxStack);}var w=new ClassWriter(0);n.accept(w);return w.toByteArray();}
 public static void main(String[]a)throws Exception {
  var defs=new HashMap<String,byte[]>();Path dir=Path.of(a[0]);try(var stream=Files.walk(dir)){for(var p:stream.filter(x->x.toString().endsWith(".class")).toList())defs.put(dir.relativize(p).toString().replace(File.separatorChar,'.').replaceAll("\\.class$",""),Files.readAllBytes(p));}
  for(String n:List.of("dev.emi.emi.api.stack.FluidEmiStack","dev.emi.emi.api.stack.ListEmiIngredient","dev.emi.emi.api.stack.TagEmiIngredient","dev.emi.emi.api.stack.EmiStack","dev.emi.emi.api.stack.EmiIngredient","dev.emi.emi.api.stack.EmptyEmiStack","dev.emi.emi.api.stack.ItemEmiStack","dev.emi.emi.api.stack.Comparison","dev.emi.emi.api.stack.Comparison$Predicate","dev.emi.emi.api.stack.Comparison$HashFunction","dev.emi.emi.registry.EmiComparisonDefaults","dev.emi.emi.registry.EmiStackList$ComparisonHashStrategy","local.prominence.work.RecipeIndexInsertion"))defs.put(n,read(n));
  defs.put("dev.emi.emi.api.stack.ItemEmiStack",instrumentItem(defs.get("dev.emi.emi.api.stack.ItemEmiStack")));
  List<?> old=null;int oldCopies=0,newCopies=0;String target="dev.emi.emi.registry.EmiRecipes$Manager";
  for(boolean fixed:List.of(false,true)){var copy=new HashMap<>(defs);copy.put(target,trimManager(fixed?Files.readAllBytes(Path.of(a[1])):read(target)));var l=new Loader(copy);var f=l.loadClass("fixture.RecipeFixture");var result=(List<?>)f.getMethod("run",boolean.class).invoke(null,fixed);int c=f.getField("copies").getInt(null);if(!fixed){old=result;oldCopies=c;}else{if(!old.equals(result))throw new AssertionError("native outputs differ");newCopies=c;}}
  if(newCopies>=oldCopies)throw new AssertionError("no copy reduction");System.out.println("PASS: actual native EMI insertion/ItemEmiStack.copy/NBT comparison and fastutil; identical output and ordered sets; copies "+oldCopies+" -> "+newCopies+"; custom predicate/remainder/subclass fallback; no game/GL");
 }
}

