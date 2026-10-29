package local.prominence.work;

import com.google.gson.*;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.widget.SlotWidget;
import fzzyhmstrs.emi_loot.util.ConditionalStack;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.*;

/** Display-only probability: conditional table distribution and independent chance gates. */
public final class LootDropTooltip {
    private LootDropTooltip(){}
    private static final class Data {
        static final JsonObject TABLES=load();
        static JsonObject load(){try(var in=LootDropTooltip.class.getResourceAsStream("/assets/prominence_korean_compat/loot_chances.json")){
            return JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonObject("tables");
        }catch(Exception e){return new JsonObject();}}
    }
    public static String table(String recipe){var parts=recipe.split("/",4);return parts.length==4?parts[2]+":"+parts[3]:"";}
    public static Double hit(String table,String item,double expected){
        var t=Data.TABLES.getAsJsonObject(table);if(t==null||!t.has(item))return null;
        var row=t.getAsJsonArray(item);return Math.abs(row.get(0).getAsDouble()-expected)<=Math.max(.0001,Math.abs(expected)*.00001)?row.get(1).getAsDouble():null;
    }
    public static Double chestHit(String table,String item,double expected){var h=hit(table,item,expected);if(h==null)return null;var r=Data.TABLES.getAsJsonObject(table).getAsJsonArray(item);return r.size()<3||r.get(2).isJsonNull()?null:h*r.get(2).getAsDouble();}
    public static String shortBeheading(String name,boolean ko){return ko?"엘레멘티움 도끼로 "+name+" 처치":"Kill "+name+" with an Elementium Axe";}
    private static String key(class_2561 t){return t.method_10851() instanceof class_2588 c?c.method_11022():"";}
    private static Object[] args(class_2561 t){return t.method_10851() instanceof class_2588 c?c.method_11023():new Object[0];}
    private static String string(Object o){return o instanceof class_2561 t?t.getString():String.valueOf(o);}
    private static double numeric(Object o){return Double.parseDouble(string(o));}
    private static boolean chance(class_2561 t){return key(t).equals("emi_loot.condition.chance")||key(t).equals("emi_loot.condition.chance_looting");}
    private static String victim(LootEntityRule.Node n){if(n.key().equals("emi_loot.entity_predicate.type_single")&&n.args().size()==1&&n.args().get(0).key().startsWith("entity."))return n.args().get(0).key();for(var a:n.args()){var v=victim(a);if(v!=null)return v;}for(var a:n.siblings()){var v=victim(a);if(v!=null)return v;}return null;}
    private static class_2561 compact(class_2561 t,boolean ko){
        if(!ko)return t;
        String k=key(t);var a=args(t);
        if((k.equals("emi_loot.condition.entity_props")||k.equals("emi_loot.entity_predicate.base")||k.equals("emi_loot.entity_predicate.type_single"))&&a.length==1&&t.method_10855().isEmpty())return compactArg(a[0],ko);
        if(k.equals("emi_loot.entity_predicate.entity_this")&&a.length==1&&t.method_10855().isEmpty())return class_2561.method_43470("대상: "+compactArg(a[0],ko).getString());
        if(k.equals("emi_loot.entity_predicate.entity_killer")&&a.length==1&&t.method_10855().isEmpty())return class_2561.method_43470("처치자: "+compactArg(a[0],ko).getString());
        return t;
    }
    private static class_2561 compactArg(Object a,boolean ko){return a instanceof class_2561 t?compact(t,ko):class_2561.method_43470(String.valueOf(a));}
    public static List<class_2561> lines(String recipe,ConditionalStack entry,boolean ko){
        return lines(recipe,entry.getStacks().stream().map(s->class_7923.field_41178.method_10221(s.getItemStack().method_7909()).toString()).toList(),entry.weight(),entry.conditions().stream().map(p->p.method_15441()).toList(),ko,class_1074::method_4662);
    }
    public static List<class_2561> lines(String recipe,List<String> items,double expected,List<class_2561> conditions,boolean ko,java.util.function.Function<String,String> names){
        var nodes=conditions.stream().map(t->LootEntityDisplay.node(t,0)).toList();
        boolean axe=nodes.stream().anyMatch(n->LootEntityRule.contains(n,"item.botania.elementium_axe"));
        Double conditional=null;
        for(var item:items){
            Double h=hit(table(recipe),item,expected);
            // Botania's reviewed beheading pools have exactly one draw; entity/weapon
            // predicates remain mandatory, and random-chance-with-looting is separate.
            if(h==null&&axe)h=hit("botania:elementium_axe_beheading",item,expected);
            if(h==null){conditional=null;break;}if(conditional!=null&&Math.abs(conditional-h)>.0001){conditional=null;break;}conditional=h;
        }
        double base=conditional==null?Double.NaN:conditional;double inc=0;int gates=0;double[] levels={base,base,base,base};
        var other=new ArrayList<class_2561>();
        for(var c:conditions){
            if(chance(c)&&c.method_10855().isEmpty()){
                try{var a=args(c);double p=numeric(a[0]);double l=a.length>1?numeric(a[1]):0;
                    if(!Double.isFinite(p)||p<0||p>100||!Double.isFinite(l)||l<0)throw new NumberFormatException();
                    for(int i=0;i<levels.length;i++)if(Double.isFinite(levels[i])){
                        var factor=java.math.BigDecimal.valueOf(p).add(java.math.BigDecimal.valueOf(l).multiply(java.math.BigDecimal.valueOf(i))).min(java.math.BigDecimal.valueOf(100));
                        levels[i]=java.math.BigDecimal.valueOf(levels[i]).multiply(factor).movePointLeft(2).doubleValue();
                    }
                    base=levels[0];inc=Double.isFinite(base)?java.math.BigDecimal.valueOf(levels[1]).subtract(java.math.BigDecimal.valueOf(base)).doubleValue():0;
                    gates++;
                    if(conditional==null)other.add(class_2561.method_43470((ko?"조건 확률: ":"Condition chance: ")+LootNumbers.percent(p)+"%"+(l>0?(ko?" · 약탈 레벨당 +":" · per Looting level +")+LootNumbers.percent(l)+"%p":"")));
                }catch(Exception e){base=Double.NaN;other.add(c);}
            }else if(!key(c).equals("emi_loot.no_conditions")){
                var n=LootEntityDisplay.node(c,0);if(LootEntityRule.contains(n,"emi_loot.condition.chance")||LootEntityRule.contains(n,"emi_loot.condition.chance_looting")){base=Double.NaN;Arrays.fill(levels,Double.NaN);}
                other.add(c);
            }
        }
        var result=new ArrayList<class_2561>();
        String probability=Double.isFinite(base)?LootNumbers.percent(Math.min(100,base))+"%":(ko?"확인 불가":"unavailable");
        String label=(ko?"드롭 확률: ":"Drop chance: ")+probability;
        if(Double.isFinite(base)&&inc>0){
            if(Math.abs((levels[2]-levels[1])-inc)<.00001&&Math.abs((levels[3]-levels[2])-inc)<.00001)label+=(ko?" · 약탈 레벨당 +":" · per Looting level +")+LootNumbers.percent(inc)+"%p";
            else label+=(ko?" · 약탈 I/II/III: ":" · Looting I/II/III: ")+LootNumbers.percent(levels[1])+"/"+LootNumbers.percent(levels[2])+"/"+LootNumbers.percent(levels[3])+"%";
        }
        result.add(class_2561.method_43470(label));
        String victim=nodes.stream().map(LootDropTooltip::victim).filter(Objects::nonNull).findFirst().orElse(null);
        boolean head=!items.isEmpty()&&items.stream().allMatch(item->hit("botania:elementium_axe_beheading",item,expected)!=null);
        if(ko&&axe&&head&&victim!=null){
            result.add(class_2561.method_43470(shortBeheading(names.apply(victim),ko)));
            for(var c:other)if(!key(c).equals("emi_loot.condition.entity_props"))result.add(compact(c,ko));
        }else for(var c:other)result.add(compact(c,ko));
        return result;
    }
    public static SlotWidget appendChest(SlotWidget slot,double expected,EmiRecipe recipe){
        var id=recipe.getId();Double probability=null;
        for(var s:slot.getStack().getEmiStacks()){
            var item=class_7923.field_41178.method_10221(s.getItemStack().method_7909()).toString();
            var h=chestHit(table(id==null?"":id.toString()),item,expected);
            if(h==null||probability!=null&&Math.abs(probability-h)>.0001){probability=null;break;}probability=h;
        }
        return slot.appendTooltip(class_2561.method_43470((LootUiText.korean()?"드롭 확률: ":"Drop chance: ")+(probability==null?(LootUiText.korean()?"확인 불가":"unavailable"):LootNumbers.percent(probability)+"%")));
    }
    public static SlotWidget append(SlotWidget slot,ConditionalStack entry,EmiRecipe recipe){
        var id=recipe.getId();for(var line:lines(id==null?"":id.toString(),entry,LootUiText.korean()))slot.appendTooltip(line);return slot;
    }
}
