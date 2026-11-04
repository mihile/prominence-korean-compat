package local.prominence.work;
import com.google.gson.JsonParser;import java.io.InputStreamReader;import java.nio.charset.StandardCharsets;import java.nio.file.Files;import java.util.*;import net.fabricmc.loader.api.FabricLoader;import local.prominence.join.JoinGuard;
/** Independently disable individual direct-drop observations when their mod changes. */
public final class DirectLootGuard {
 private DirectLootGuard(){}
 private static final Map<String,Boolean> MATCHES=load();
 private static Map<String,Boolean> load(){var result=new HashMap<String,Boolean>();try(var in=DirectLootGuard.class.getResourceAsStream("/prominence-direct-loot-pins.json")){
  var pins=JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonObject();var loader=FabricLoader.getInstance();
  for(var entry:pins.entrySet()){boolean match=JoinGuard.matches(entry.getValue().getAsJsonObject(),id->loader.getModContainer(id).map(m->m.getMetadata().getVersion().getFriendlyString()).orElse(null),(id,path)->{try{return Files.readAllBytes(loader.getModContainer(id).orElseThrow().findPath(path).orElseThrow());}catch(Exception e){return null;}});result.put(entry.getKey(),match);}
 }catch(Exception|LinkageError e){return Map.of();}return Map.copyOf(result);}
 public static boolean allowed(String key){return MATCHES.getOrDefault(key,false);}
}
