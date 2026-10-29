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
        check(changed.get("attribute.name.generic.luck").equals("운"),"Generic Luck distinguished from Fortune");
        check(changed.get("attribute.puffish_attributes.fortune").equals("채굴 행운"),"Mining Fortune attribute distinguished");
        check(changed.get("effect.herbalbrews.fortune").equals("운 상승"),"Herbal Brews Fortune actually modifies Luck");
        check(changed.get("text.skills.luck+0.25.title").equals("§a+0.25 §7운"),"Skill Luck title uses the attribute term");
        var enchants=TranslationOverrides.apply(Map.of("enchantment.minecraft.fortune","행운","enchantment.minecraft.looting","약탈"),List.of("ko_kr"));
        check(enchants.get("enchantment.minecraft.fortune").equals("행운")&&enchants.get("enchantment.minecraft.looting").equals("약탈"),"Vanilla enchantments retain separate names");
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
        try(var in=TranslationOverrideTests.class.getResourceAsStream("/assets/prominence_korean_compat/quest_reviewed_text.json")){
            var quests=com.google.gson.JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonObject();
            String old="&7이 배낭을 장착하면 행운이 1 증가합니다.";
            check(quests.get(old).getAsString().equals(quests.get("&7This backpack gives + 1 Luck if worn").getAsString())&&quests.get(old).getAsString().contains("운&7이 1"),"English and existing Korean backpack quests match");
            String reforge="&7It's not all about grinding! You can &eincrease chances on higher qualities&7 with your &aLuck&7! Items that give luck passively, luck potions, trinkets, wine, get them all!";
            check(quests.get(reforge).getAsString().contains("&a운&7")&&!quests.get(reforge).getAsString().contains("행운"),"Reforging quest uses generic Luck");
            check(quests.get("&7Good luck.").getAsString().equals("&7행운을 빕니다."),"Narrative good-luck wish preserved");
        }
        try(var in=TranslationOverrideTests.class.getResourceAsStream("/resourcepacks/integrated_korean/assets/zenith/patchouli_books/chronicle/ko_kr/entries/potion/recipes/luck.json")){
            var book=com.google.gson.JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonObject();
            check(book.get("name").getAsString().equals("운 상승"),"Bundled book effect terminology");
            check(book.getAsJsonArray("pages").get(1).getAsJsonObject().get("text").getAsString().contains("행운의 토끼발"),"Proper ingredient name preserved");
        }
        System.out.println("PASS: "+checks+" JAR translation priority, locale, preservation, immutable-data and malformed/format checks");
    }
}
