package local.prominence.backpackinline.client;

import draylar.tiered.api.TieredItemTags;
import elocindev.tierify.Tierify;
import elocindev.tierify.data.ReforgeDataLoader;
import java.util.ArrayList;
import java.util.List;
import local.prominence.backpackinline.InlineState;
import net.minecraft.*;

/** The native Tierify screen's material lookup and warning order, using live synchronized data. */
public final class InlineReforgeTooltip {
    private InlineReforgeTooltip(){}
    public static List<class_2561> lines(InlineState state){
        return lines(state.inventory().method_5438(1),state.inventory().method_5438(0),state.ready(),Tierify.REFORGE_DATA_LOADER);
    }
    static List<class_1792> materials(class_1799 gear,ReforgeDataLoader data){
        if(gear.method_7960())return List.of();
        var item=gear.method_7909();var explicit=data.getReforgeBaseItems(item);
        if(!explicit.isEmpty())return explicit;
        var result=new ArrayList<class_1792>();
        if(item instanceof class_1831 tool){
            for(var stack:tool.method_8022().method_8023().method_8105())result.add(stack.method_7909());
        }else if(item instanceof class_1738 armor&&armor.method_7686().method_7695()!=null){
            for(var stack:armor.method_7686().method_7695().method_8105())result.add(stack.method_7909());
        }else{
            for(var entry:class_7923.field_41178.method_40260(TieredItemTags.REFORGE_BASE_ITEM))result.add(entry.comp_349());
        }
        return result;
    }
    static List<class_2561> lines(class_1799 gear,class_1799 material,boolean ready,ReforgeDataLoader data){
        var result=new ArrayList<class_2561>();var items=materials(gear,data);
        if(!items.isEmpty()&&(material.method_7960()||!items.contains(material.method_7909()))){
            result.add(class_2561.method_43471("screen.tiered.reforge_ingredient"));
            for(var item:items)result.add(item.method_7848());
        }
        if(!gear.method_7960()&&gear.method_7963()&&gear.method_7986())result.add(class_2561.method_43471("screen.tiered.reforge_damaged"));
        if(result.isEmpty())result.add(class_2561.method_43471(ready?"prominence.inline.reforge":"prominence.inline.inputs"));
        return result;
    }
}
