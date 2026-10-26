package local.prominence.localization;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
public final class TranslationOverrideTests {
    private static int checks;
    static void check(boolean condition,String why){checks++;if(!condition)throw new AssertionError(why);}
    static InputStream json(String raw){return new ByteArrayInputStream(raw.getBytes(StandardCharsets.UTF_8));}
    public static void main(String[]args)throws Exception{
        var base=Map.of("item.croptopia.calamari","오징어","unrelated","keep");
        var changed=TranslationOverrides.apply(base,List.of("en_us","ko_kr"));
        check(changed.get("item.croptopia.calamari").equals("오징어 다리"),"Old Paxi value overridden");
        check(changed.get("unrelated").equals("keep"),"Unrelated loaded translation preserved");
        check(base.get("item.croptopia.calamari").equals("오징어"),"Input map not mutated");
        check(TranslationOverrides.apply(base,List.of("en_us"))==base,"English untouched");
        check(TranslationOverrides.apply(base,List.of("en_us","ja_jp"))==base,"Other locale untouched");
        check(TranslationOverrides.apply(base,List.of("ko_kr","en_us"))==base,"Last selected language wins");
        check(TranslationOverrides.apply(base,List.of())==base,"Empty fallback untouched");
        check(TranslationOverrides.merge(base,Map.of())==base,"Empty correction fallback");
        var parsed=TranslationOverrides.read(json("{\"value\":\"피해 %1$.2f, %2$d, %s, 10%%\"}"));
        check(parsed.get("value").equals("피해 %1$s, %2$s, %s, 10%%"),"Native placeholder normalization");
        try{parsed.put("x","y");throw new AssertionError("Immutable source expected");}catch(UnsupportedOperationException good){checks++;}
        for(String bad:List.of("[]","{\"k\":2}","{\"k\":null}","{\"\":\"x\"}","{")){
            try{TranslationOverrides.read(json(bad));throw new AssertionError("Malformed input accepted: "+bad);}catch(IOException good){checks++;}
        }
        check(TranslationOverrides.apply(Map.of(),List.of("en_us","ko_kr")).containsKey("dialogue.skellak.molten_core.1_2"),"New keys inserted without pack update");
        System.out.println("PASS: "+checks+" JAR translation priority, locale, preservation, immutable-data and malformed/format checks");
    }
}
