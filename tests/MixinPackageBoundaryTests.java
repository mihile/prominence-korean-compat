import com.google.gson.*;import java.nio.file.*;import java.util.*;import java.util.zip.*;import org.objectweb.asm.*;import org.objectweb.asm.tree.*;
/** Check ordinary helpers as well as configured target transformations. No game/bootstrap. */
public final class MixinPackageBoundaryTests {
 public static void main(String[] args)throws Exception{
  try(var z=new ZipFile(args[0])){
   var prefixes=new HashSet<String>();var mixins=new HashSet<String>();var classes=new HashMap<String,ClassNode>();
   for(var entries=z.entries();entries.hasMoreElements();){var e=entries.nextElement();String n=e.getName();if(n.endsWith(".mixins.json")){
    var j=JsonParser.parseString(new String(z.getInputStream(e).readAllBytes(),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();if(j.has("package"))prefixes.add(j.get("package").getAsString().replace('.','/')+"/");
   }else if(n.endsWith(".class")&&!n.startsWith("META-INF/")){var node=new ClassNode();new ClassReader(z.getInputStream(e)).accept(node,ClassReader.SKIP_CODE|ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);classes.put(node.name,node);var ann=new ArrayList<AnnotationNode>();if(node.visibleAnnotations!=null)ann.addAll(node.visibleAnnotations);if(node.invisibleAnnotations!=null)ann.addAll(node.invisibleAnnotations);if(ann.stream().anyMatch(a->a.desc.equals("Lorg/spongepowered/asm/mixin/Mixin;")))mixins.add(node.name);}}
   var invalid=new ArrayList<String>();for(String n:classes.keySet())if(prefixes.stream().anyMatch(n::startsWith)&&!mixins.contains(n)){
    String outer=n.contains("$")?n.substring(0,n.indexOf('$')):n;if(!mixins.contains(outer))invalid.add(n);
   }
   if(!invalid.isEmpty())throw new AssertionError("Ordinary classes in Mixin-only packages: "+invalid);
   System.out.println("PASS "+classes.size()+" cumulative classes / "+prefixes.size()+" Mixin package boundaries; ordinary helpers remain loadable, no game");
  }
 }
}
