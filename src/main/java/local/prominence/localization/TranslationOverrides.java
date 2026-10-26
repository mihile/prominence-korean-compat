package local.prominence.localization;

import com.google.gson.JsonParser;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

/** Reviewed, cumulative language-key corrections carried by the signed compatibility JAR. */
public final class TranslationOverrides {
    private static final Pattern NUMERIC_FORMAT=Pattern.compile("%(\\d+\\$)?[\\d\\.]*[df]");
    private static final class Data {static final Map<String,String> KO=load();}
    static Map<String,String> read(InputStream input)throws IOException {
        if(input==null)throw new IOException("Missing translation corrections");
        try(var reader=new InputStreamReader(input,StandardCharsets.UTF_8)){
            var root=JsonParser.parseReader(reader);
            if(!root.isJsonObject())throw new IOException("Translation corrections must be an object");
            var out=new LinkedHashMap<String,String>();
            for(var entry:root.getAsJsonObject().entrySet()){
                if(entry.getKey().isBlank()||!entry.getValue().isJsonPrimitive()||!entry.getValue().getAsJsonPrimitive().isString())throw new IOException("Invalid language correction "+entry.getKey());
                // Same numeric-placeholder normalization as Minecraft 1.20.1 Language.load.
                out.put(entry.getKey(),NUMERIC_FORMAT.matcher(entry.getValue().getAsString()).replaceAll("%$1s"));
            }
            return Map.copyOf(out);
        }catch(RuntimeException malformed){throw new IOException("Malformed language corrections",malformed);}
    }
    private static Map<String,String> load(){
        try{
            var result=read(TranslationOverrides.class.getResourceAsStream("/assets/prominence_korean_compat/translation_overrides/ko_kr.json"));
            org.slf4j.LoggerFactory.getLogger("Prominence compatibility").info("JAR Korean translation corrections: {} keys",result.size());
            return result;
        }catch(IOException e){org.slf4j.LoggerFactory.getLogger("Prominence compatibility").warn("Cannot read JAR translation corrections; original translations retained",e);return Map.of();}
    }
    public static Map<String,String> apply(Map<String,String> loaded,List<String> languages){
        if(languages.isEmpty()||!"ko_kr".equals(languages.get(languages.size()-1)))return loaded;
        return merge(loaded,Data.KO);
    }
    static Map<String,String> merge(Map<String,String> loaded,Map<String,String> corrections){
        if(corrections.isEmpty())return loaded;
        var merged=new HashMap<>(loaded);merged.putAll(corrections);return merged;
    }
}
