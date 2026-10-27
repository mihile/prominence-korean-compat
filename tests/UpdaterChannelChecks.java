import java.nio.file.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
public class UpdaterChannelChecks {
 static ClassNode read(Path root,String name)throws Exception{var node=new ClassNode();new ClassReader(Files.readAllBytes(root.resolve("local/prominence/autoupdate/"+name+".class"))).accept(node,0);return node;}
 static MethodNode method(ClassNode c,String name){return c.methods.stream().filter(m->m.name.equals(name)).findFirst().orElseThrow();}
 static boolean invokes(MethodNode m,String owner,String name){for(var i:m.instructions)if(i instanceof MethodInsnNode n&&n.owner.equals(owner)&&n.name.equals(name))return true;return false;}
 static boolean constant(MethodNode m,String x){for(var i:m.instructions)if(i instanceof LdcInsnNode n&&x.equals(n.cst))return true;return false;}
 static void ok(boolean x,String why){if(!x)throw new AssertionError(why);}
 public static void main(String[] args)throws Exception{
  var root=Path.of(args[0]);var client=read(root,"UpdateClient");var check=read(root,"UpdateCheckScreen");var offer=read(root,"UpdateScreen");
  ok(invokes(method(client,"openCheck"),"local/prominence/autoupdate/UpdateClient","lookup"),"opening does not check saved channel");
  ok(!invokes(method(client,"openChoose"),"local/prominence/autoupdate/UpdateClient","lookup"),"chooser cannot stay visible");
  ok(invokes(method(client,"setTrack"),"local/prominence/autoupdate/UpdateFiles","writeProperties"),"choice not persisted");
  var init=method(check,"method_25426");ok(constant(init,"track_main_selected")&&constant(init,"track_nightly_selected"),"selected labels missing");
  ok(invokes(method(check,"select"),"local/prominence/autoupdate/UpdateClient","openCheck"),"selection does not check");
  ok(!invokes(method(check,"select"),"local/prominence/autoupdate/UpdateClient","lookup"),"duplicate request");
  init=method(offer,"method_25426");ok(constant(init,"channel_nightly")&&constant(init,"channel_main"),"release channel label missing");
  ok(offer.methods.stream().filter(m->m.name.startsWith("lambda$")).anyMatch(m->invokes(m,"local/prominence/autoupdate/UpdateClient","openChoose")),"no channel change action");
  System.out.println("PASS 8 compiled channel selection, saved lookup and navigation checks; no game screen started");
 }
}
