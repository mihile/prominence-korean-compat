package local.prominence.infusion;

import elocindev.eldritch_end.api.infusion.InfusableItemMaterial;
import elocindev.eldritch_end.api.infusion.InfusionTemplate;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.*;

/** Replace native armor infusions without re-running the native append-only modifier merge. */
public final class InfusionReplacement {
    private static final Set<String> MATERIALS=Set.of("eldritch_end:etyr_ingot",
        "eldritch_end:aberration_limb","eldritch_end:aberration_heart","eldritch_end:xal");

    public static class_1799 preview(class_1799 template,class_1799 equipment,class_1799 addition) {
        if(template.method_7960()||equipment.method_7960()||addition.method_7960()
            ||!(equipment.method_7909() instanceof class_1738 armor)
            ||!(addition.method_7909() instanceof InfusableItemMaterial material)
            ||!(template.method_7909() instanceof InfusionTemplate allowed))return null;
        var root=equipment.method_7969();
        if(root==null||!root.method_10573("eldritch_infusions",10))return null;
        var infusion=root.method_10562("eldritch_infusions");
        String from=infusion.method_10558("materialIdentifier");
        String to=class_7923.field_41178.method_10221(addition.method_7909()).toString();
        if(!infusion.method_10577("isInfused")||!MATERIALS.contains(from)||!MATERIALS.contains(to)
            ||from.equals(to)||material.getInfusionTemplate()!=template.method_7909()
            ||!material.applyToArmor()||!material.isInfusable()||!allowed.isEquipmentAllowed(equipment))return null;
        if(!(class_7923.field_41178.method_10223(new class_2960(from)) instanceof InfusableItemMaterial previous))return null;
        var owned=new HashSet<String>();
        for(var holder:previous.getInfusionAttributes())
            owned.add(class_7923.field_41190.method_10221(holder.attribute).toString());
        var result=equipment.method_7972();
        // Native application always writes an explicit armor modifier list. Unknown/malformed
        // ownership is left untouched instead of guessing or removing another mod's upgrades.
        if(stripPriorInfusion(result.method_7969(),owned,armor.method_7685().method_5923())==0)return null;
        var state=result.method_7911("eldritch_infusions");
        state.method_10556("isInfused",true);
        state.method_10582("currentInfusion",class_7923.field_41178.method_10221(addition.method_7909()).method_12832());
        state.method_10582("materialIdentifier",to);
        for(var holder:material.getInfusionAttributes())result.method_7916(holder.attribute,
            new class_1322(material.getInfusionUUID(addition,armor.method_7685()),"Infusion modifier",holder.amount,holder.operation),armor.method_7685());
        return result;
    }

    /** Only the previous native infusion's named modifiers on this armor slot are owned here. */
    public static int stripPriorInfusion(class_2487 root,Set<String> owned,String slot) {
        if(root==null||!root.method_10573("AttributeModifiers",9))return 0;
        var modifiers=root.method_10554("AttributeModifiers",10);
        int removed=0;
        for(int i=modifiers.size()-1;i>=0;i--) {
            var entry=modifiers.method_10602(i);
            if(entry.method_10558("Name").equals("Infusion modifier")
                &&owned.contains(entry.method_10558("AttributeName"))
                &&entry.method_10558("Slot").equals(slot)) {
                modifiers.method_10536(i);removed++;
            }
        }
        return removed;
    }
    private InfusionReplacement(){}
}
