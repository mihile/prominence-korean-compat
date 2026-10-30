package local.prominence.arrival;
import com.google.gson.JsonParser;import java.nio.file.*;import java.util.*;
public final class ArrivalGuardTests {
 public static void main(String[] args)throws Exception{
  var pins=JsonParser.parseString(Files.readString(Path.of(args[0]))).getAsJsonObject();var versions=new HashMap<String,String>();versions.put("minecraft","1.20.1");versions.put("mixinextras","0.5.4");var classes=new HashMap<String,byte[]>();
  for(var entry:pins.entrySet()){versions.put(entry.getKey(),entry.getValue().getAsJsonObject().get("version").getAsString());for(var cl:entry.getValue().getAsJsonObject().getAsJsonObject("classes").entrySet())try(var in=ArrivalGuardTests.class.getClassLoader().getResourceAsStream(cl.getKey())){classes.put(cl.getKey(),in.readAllBytes());}}
  if(!ArrivalGuard.matches(pins,versions::get,(id,path)->classes.get(path)))throw new AssertionError("Installed native pins rejected");
  if(ArrivalGuard.matches(pins,versions::get,(id,path)->null))throw new AssertionError("Missing native class accepted");
  if(ArrivalGuard.matches(pins,versions::get,(id,path)->new byte[]{1}))throw new AssertionError("Modified native class accepted");
  versions.put("fwaystones","3.3.4");if(ArrivalGuard.matches(pins,versions::get,(id,path)->classes.get(path)))throw new AssertionError("Mod drift accepted");
  versions.put("fwaystones","3.3.3+mc1.20.1");versions.put("minecraft","1.21");if(ArrivalGuard.matches(pins,versions::get,(id,path)->classes.get(path)))throw new AssertionError("MC drift accepted");
  versions.put("minecraft","1.20.1");versions.put("mixinextras","0.5.5");if(ArrivalGuard.matches(pins,versions::get,(id,path)->classes.get(path)))throw new AssertionError("MixinExtras drift accepted");
  System.out.println("PASS six arrival native fingerprint/version/missing/drift guard cases; no game.");
 }
}
