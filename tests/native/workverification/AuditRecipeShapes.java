package workverification;
import org.objectweb.asm.*;import org.objectweb.asm.tree.*;import java.util.*;import java.util.zip.*;import java.nio.file.*;import com.google.gson.*;
public class AuditRecipeShapes{
 static Map<String,ClassNode> nodes=new HashMap<>();static Set<String> used=new HashSet<>();
 static MethodNode find(String cls,String name){var c=nodes.get(cls);if(c==null)return null;for(var m:c.methods)if(m.name.equals(name)&&m.desc.equals("()Ljava/util/List;")){used.add(cls);return m;}var v=find(c.superName,name);if(v!=null)return v;for(var i:c.interfaces){v=find(i,name);if(v!=null)return v;}return null;}
 static String owner(MethodNode m){for(var e:nodes.entrySet())if(e.getValue().methods.contains(m))return e.getKey();throw new IllegalStateException();}
 public static void main(String[]a)throws Exception{try(var z=new ZipFile(a[0])){for(var e:Collections.list(z.entries()))if(e.getName().endsWith(".class")){var n=new ClassNode();new ClassReader(z.getInputStream(e).readAllBytes()).accept(n,0);nodes.put(n.name,n);}}
 var result=new TreeMap<String,List<String>>();if(a.length>3)try(var z=new ZipFile(a[3])){for(var e:Collections.list(z.entries()))if(e.getName().endsWith(".class")){var n=new ClassNode();new ClassReader(z.getInputStream(e).readAllBytes()).accept(n,0);nodes.put(n.name,n);}}
for(var n:nodes.values())if((n.access&(Opcodes.ACC_ABSTRACT|Opcodes.ACC_INTERFACE))==0&&(n.name.startsWith("dev/emi/emi/recipe/")||n.name.equals("dev/emi/emi/api/recipe/EmiCraftingRecipe")||n.name.equals("aztech/modern_industrialization/compat/viewer/impl/emi/ViewerCategoryEmi$ViewerRecipe"))){var fields=new ArrayList<String>();boolean ok=true;var trial=new HashSet<String>();used=trial;for(var method:List.of("getInputs","getOutputs","getCatalysts")){var m=find(n.name,method);if(m==null){ok=false;break;}var ops=new ArrayList<AbstractInsnNode>();for(var op:m.instructions)if(op.getOpcode()>=0)ops.add(op);if(ops.isEmpty()||ops.get(ops.size()-1).getOpcode()!=Opcodes.ARETURN){ok=false;break;}
 for(var op:ops){if(op instanceof VarInsnNode v&&v.getOpcode()==Opcodes.ALOAD&&v.var==0)continue;
 if(op instanceof FieldInsnNode f&&f.getOpcode()==Opcodes.GETFIELD){fields.add(f.owner.replace('/','.')+"#"+f.name);trial.add(f.owner);continue;}
 if(op instanceof MethodInsnNode call&&call.getOpcode()==Opcodes.INVOKESTATIC&&call.owner.equals("java/util/List")&&call.name.equals("of"))continue;
 if(op.getOpcode()==Opcodes.ARETURN)continue;ok=false;break;}if(!ok)break;}

 if(ok){result.put(n.name.replace('/','.'),fields);}}
 Files.writeString(Path.of(a[1]),new GsonBuilder().setPrettyPrinting().create().toJson(result)+"\n");if(a.length>2&&!JsonParser.parseString(Files.readString(Path.of(a[2]))).equals(new Gson().toJsonTree(result)))throw new AssertionError("Fixed getter shape manifest differs from actual native bytecode");System.out.println("PASS: native fixed recipe getter shapes="+result.size());}
}