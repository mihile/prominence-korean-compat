package local.prominence.memory;
import java.util.*;
/** Exercise the real optional guard with current raw classes and deliberately stale inputs. */
public final class GuardProbe {
 public static void main(String[]args)throws Exception {
  int count=0;
  for(String module:List.of("BadPackets","Ae2","Legendary","Zenith","Azure","Screens","Armor","StackKeys","Hud","ArmorRenderers")) {
   String key="retention"+module;var versions=new HashMap<String,String>();var bytes=new HashMap<String,byte[]>();
   for(var pin:WorldCacheGuard.pins(key)) {
    versions.put(pin.mod(),pin.version());
    for(String path:pin.classes().keySet())try(var in=GuardProbe.class.getClassLoader().getResourceAsStream(path)){if(in==null)throw new AssertionError(path);bytes.put(pin.mod()+"/"+path,in.readAllBytes());}
   }
   if(!WorldCacheGuard.matches(key,versions,bytes))throw new AssertionError("Current guard disabled: "+key);count++;
   String version=versions.keySet().iterator().next(),saved=versions.put(version,"changed-version");
   if(WorldCacheGuard.matches(key,versions,bytes))throw new AssertionError("Unknown version accepted");count++;versions.put(version,saved);
   String path=bytes.keySet().iterator().next();byte[] original=bytes.get(path),changed=original.clone();changed[0]^=1;bytes.put(path,changed);
   if(WorldCacheGuard.matches(key,versions,bytes))throw new AssertionError("Changed bytecode accepted");count++;bytes.remove(path);
   if(WorldCacheGuard.matches(key,versions,bytes))throw new AssertionError("Missing class accepted");count++;
  }
  System.out.println("Retention production guard checks passed: "+count);
 }
}
