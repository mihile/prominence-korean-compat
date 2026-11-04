package local.prominence.work;

import com.google.gson.*;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.class_2561;

/** Read-only explanations of fallback data; never promote an EMI mean to a probability. */
public final class LootFormula {
    private LootFormula() {}
    private static final class Data {
        static final JsonObject DETAILS=load();
        static JsonObject load() {
            try(var in=LootFormula.class.getResourceAsStream("/assets/prominence_korean_compat/loot_chances.json")) {
                var root=JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonObject();
                return root.has("details")?root.getAsJsonObject("details"):new JsonObject();
            } catch(Exception e) {return new JsonObject();}
        }
    }
    private static String type(JsonObject o,String field) {return o.has(field)?o.get(field).getAsString().replace("minecraft:",""):"";}
    private static String value(JsonElement v,boolean ko) {
        if(v==null||v.isJsonNull())return "?";
        if(v.isJsonPrimitive())return v.toString();
        if(!v.isJsonObject())return "?";
        var o=v.getAsJsonObject();String t=type(o,"type");
        if(t.equals("constant"))return value(o.get("value"),ko);
        if(t.equals("uniform")||t.isEmpty()&&o.has("min")&&o.has("max"))return (ko?"균등":"Uniform")+"("+value(o.get("min"),ko)+", "+value(o.get("max"),ko)+")";
        if(t.equals("binomial"))return (ko?"이항":"Binomial")+"("+value(o.get("n"),ko)+", "+value(o.get("p"),ko)+")";
        return t.isEmpty()?"?":t;
    }
    private static String quantity(JsonArray functions,int looting,boolean ko) {
        String q="1";
        for(var f:functions) {
            var o=f.getAsJsonObject();String t=type(o,"function");
            if(t.equals("set_count"))q=o.has("add")&&o.get("add").getAsBoolean()?"("+q+" + "+value(o.get("count"),ko)+")":value(o.get("count"),ko);
            else if(t.equals("looting_enchant")) {
                q+=" + "+(ko?"반올림":"round")+"("+value(o.get("count"),ko)+" × "+Math.max(0,looting)+")";
                if(o.has("limit")&&o.get("limit").getAsInt()>0)q="min("+q+", "+o.get("limit")+")";
            } else if(Set.of("limit_count","explosion_decay","apply_bonus","furnace_smelt").contains(t))q=t+"("+q+")";
            if(o.has("conditions")&&!o.getAsJsonArray("conditions").isEmpty())q+=(ko?" [조건부]":" [conditional]");
        }
        return q;
    }
    public static List<class_2561> lines(String table,List<String> items,int looting,double luck,boolean ko,boolean expanded) {
        var out=new ArrayList<class_2561>();
        out.add(class_2561.method_43470(ko?"계산식: P(조건 충족 ∧ 최종 드롭 수량 ≥ 1)":"Formula: P(requirements met ∧ final drop count ≥ 1)"));
        var rows=Data.DETAILS.getAsJsonObject(table);
        if(rows==null||items.isEmpty()||!rows.has(items.get(0))) {
            out.add(class_2561.method_43470(ko?"서버의 사용자 정의 추첨 정보가 필요합니다.":"Custom server roll data is required."));return out;
        }
        var pools=rows.getAsJsonArray(items.get(0));
        // A cycling icon may represent different source pools. Do not label all of it
        // with the first item's model; detailed formulas require matching structures.
        for(String item:items)if(!rows.has(item)||!pools.equals(rows.get(item))) {
            out.add(class_2561.method_43470(ko?"묶인 아이템의 추첨 방식이 서로 다릅니다.":"Grouped items have different roll rules."));return out;
        }
        out.add(class_2561.method_43470(ko?"기본 테이블 계산 구조 · 실제 조건은 아래 참조":"Base-table calculation structure · live requirements below"));
        int shown=0;
        for(var p:pools) {
            if(shown++>=3){out.add(class_2561.method_43470(ko?"추가 추첨 풀 있음":"Additional loot pools"));break;}
            var o=p.getAsJsonObject();String prefix=pools.size()>1?(ko?"풀 ":"Pool ")+shown+": ":"";
            boolean simple=o.get("simple").getAsBoolean();
            String share=simple?value(o.get("weight"),ko)+"/"+value(o.get("total"),ko):(ko?"P(조건부 항목 선택)":"P(conditional entry selection)");
            out.add(class_2561.method_43470(prefix+(ko?"추첨 ":"Draws ")+value(o.get("rolls"),ko)+" · "+(ko?"선택 비중 ":"Selection ")+share));
            out.add(class_2561.method_43470("Q = "+quantity(o.getAsJsonArray("functions"),looting,ko)));
            if(expanded) {
                if(!o.get("bonus").isJsonPrimitive()||!o.get("bonus").getAsString().equals("0"))out.add(class_2561.method_43470((ko?"추가 추첨: ⌊":"Bonus draws: ⌊")+value(o.get("bonus"),ko)+" × "+LootNumbers.number(Double.isFinite(luck)?luck:0)+"⌋"));
                var ids=new LinkedHashSet<String>();
                for(String key:List.of("conditions","entryConditions"))for(var c:o.getAsJsonArray(key))ids.add(type(c.getAsJsonObject(),"condition"));
                for(var f:o.getAsJsonArray("functions"))ids.add(type(f.getAsJsonObject(),"function"));
                if(!ids.isEmpty())out.add(class_2561.method_43470((ko?"처리: ":"Rules: ")+String.join(", ",ids)));
            }
        }
        if(pools.size()>1)out.add(class_2561.method_43470(ko?"최종 확률 = P(각 풀의 드롭 중 하나 이상 발생)":"Final chance = P(at least one pool produces the item)"));
        return out;
    }
}
