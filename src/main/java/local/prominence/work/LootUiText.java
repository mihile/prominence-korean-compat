package local.prominence.work;
import com.google.gson.*;import java.io.InputStreamReader;import java.nio.charset.StandardCharsets;import java.util.*;import net.minecraft.*;
public final class LootUiText {
 private record Title(String structure,String fallback,String detail){}
 private static final class Data {static final Map<String,Title> TITLES=load();}
 private static Map<String,Title> load(){try(var in=LootUiText.class.getResourceAsStream("/assets/prominence_korean_compat/loot_title_names.json")){var out=new HashMap<String,Title>();for(var e:JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonObject().entrySet()){var v=e.getValue().getAsJsonObject();out.put(e.getKey(),new Title(v.get("structure").getAsString(),v.get("fallback").getAsString(),v.get("detail").getAsString()));}return Map.copyOf(out);}catch(Exception e){org.slf4j.LoggerFactory.getLogger("Prominence compatibility").warn("Cannot load loot title map",e);return Map.of();}}
 public static boolean korean(){return "ko_kr".equals(class_310.method_1551().method_1526().method_4669());}
 public static class_5250 title(class_2960 id){if(!korean())return null;var t=Data.TITLES.get(id.toString());if(t==null)return null;String base=t.fallback();if(!t.structure().isEmpty()){String key="structure."+t.structure().replace(':','.').replace('/','.');if(class_1074.method_4663(key)){String translated=class_1074.method_4662(key);if(translated.chars().anyMatch(c->c>=0xAC00&&c<=0xD7A3))base=translated;}}
  return class_2561.method_43470(base+(t.detail().isBlank()?"":" · "+t.detail())+" 전리품 상자");}
 public static String dimensions(String name,class_2960 id,List<class_2960> dims){return SearchDimensions.append(name,id.toString(),dims==null?List.of():dims.stream().map(Object::toString).toList(),raw->class_1074.method_4662("dimension."+raw.replace(':','.').replace('/','.')));}
 // The slot wrapper supplies a distribution-based probability and required conditions.
 public static class_5250 expectation(String key,Object[] args){return args.length==1?class_2561.method_43470(""):null;}
}
