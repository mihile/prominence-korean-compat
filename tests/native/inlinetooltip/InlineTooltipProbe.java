package local.prominence.backpackinline.client;
import draylar.tiered.api.TieredItemTags;
import elocindev.tierify.data.ReforgeDataLoader;
import java.util.*;
import net.minecraft.*;
/** Real registry, tool/armor repair ingredients and Tierify loader; no player/world/client/GL. */
public final class InlineTooltipProbe {
    static final java.io.PrintStream REPORT=System.out;static int checks;
    static void check(boolean b,String s){checks++;if(!b)throw new AssertionError(s);}
    static class_1792 item(String name){return class_7923.field_41178.method_10223(new class_2960("minecraft",name));}
    static class_1799 stack(String name){return new class_1799(item(name));}
    static boolean has(List<class_2561> lines,String key){return lines.contains(class_2561.method_43471(key));}
    public static void main(String[]args)throws Exception {
        var diagnostic=System.err;try{run();}catch(Throwable t){t.printStackTrace(diagnostic);System.exit(1);}
    }
    static void run()throws Exception {
        net.fabricmc.loader.impl.launch.FabricLauncherBase.setLauncher((net.fabricmc.loader.impl.launch.FabricLauncher)java.lang.reflect.Proxy.newProxyInstance(InlineTooltipProbe.class.getClassLoader(),new Class[]{net.fabricmc.loader.impl.launch.FabricLauncher.class},(p,m,a)->{if(m.getName().equals("isDevelopment"))return false;throw new UnsupportedOperationException(m.getName());}));
        class_155.method_36208();class_2966.method_12851();
        var loader=new ReforgeDataLoader();var gear=stack("diamond_pickaxe");
        check(InlineReforgeTooltip.materials(class_1799.field_8037,loader).isEmpty(),"Empty equipment has no materials");
        check(InlineReforgeTooltip.materials(gear,loader).equals(List.of(item("diamond"))),"Native diamond tool repair material");
        check(InlineReforgeTooltip.materials(stack("iron_chestplate"),loader).equals(List.of(item("iron_ingot"))),"Native armor repair material");
        var lines=InlineReforgeTooltip.lines(gear,class_1799.field_8037,false,loader);
        check(has(lines,"screen.tiered.reforge_ingredient")&&lines.contains(item("diamond").method_7848()),"Missing material lists actual localized name");
        check(!has(InlineReforgeTooltip.lines(gear,stack("diamond"),true,loader),"screen.tiered.reforge_ingredient"),"Native matching material suppresses missing-material warning");
        check(has(InlineReforgeTooltip.lines(gear,stack("iron_ingot"),false,loader),"screen.tiered.reforge_ingredient"),"Wrong material still lists requirement");
        loader.putReforgeBaseItems(new class_2960("minecraft","diamond_pickaxe"),List.of(item("netherite_ingot"),item("gold_ingot")));
        check(InlineReforgeTooltip.materials(gear,loader).equals(List.of(item("netherite_ingot"),item("gold_ingot"))),"Tierify override takes precedence over native repair ingredient");
        lines=InlineReforgeTooltip.lines(gear,class_1799.field_8037,false,loader);
        check(lines.size()==3&&lines.get(1).equals(item("netherite_ingot").method_7848())&&lines.get(2).equals(item("gold_ingot").method_7848()),"All explicit alternatives preserve native order");
        loader.putReforgeBaseItems(new class_2960("minecraft","diamond_pickaxe"),List.of(item("emerald")));
        check(InlineReforgeTooltip.materials(gear,loader).equals(List.of(item("emerald"))),"Live data replacement refreshes same equipment stack");
        gear.method_7974(1);lines=InlineReforgeTooltip.lines(gear,class_1799.field_8037,false,loader);
        check(has(lines,"screen.tiered.reforge_damaged"),"Damaged gear warning preserved");
        check(lines.get(lines.size()-1).equals(class_2561.method_43471("screen.tiered.reforge_damaged")),"Native material then repair-warning order");
        check(gear.method_7919()==1,"Lookup does not repair or change gear");
        loader.clearReforgeBaseItems();check(InlineReforgeTooltip.materials(gear,loader).equals(List.of(item("diamond"))),"Reload clear falls back immediately");
        class_7923.field_41178.method_40257(Map.of(TieredItemTags.REFORGE_BASE_ITEM,List.of(item("iron_ingot").method_40131(),item("gold_ingot").method_40131())));
        check(InlineReforgeTooltip.materials(stack("apple"),loader).equals(List.of(item("iron_ingot"),item("gold_ingot"))),"Generic equipment uses live native fallback tag");
        check(InlineReforgeTooltip.materials(stack("iron_chestplate"),loader).equals(List.of(item("iron_ingot"))),"Armor ingredient still precedes generic tag");
        check(has(InlineReforgeTooltip.lines(class_1799.field_8037,class_1799.field_8037,false,loader),"prominence.inline.inputs"),"Empty panel retains concise guidance");
        check(has(InlineReforgeTooltip.lines(stack("diamond_pickaxe"),stack("diamond"),true,loader),"prominence.inline.reforge"),"Ready button retains action label");
        REPORT.println("PASS: "+checks+" native Tierify/material tooltip checks; actual custom data, tool/armor ingredients, live tags and item metadata; no game/world/GL.");
    }
}
