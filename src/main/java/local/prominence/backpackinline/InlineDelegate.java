package local.prominence.backpackinline;

import elocindev.tierify.screen.ReforgeScreenHandler;
import local.prominence.backpackinline.mixin.ReforgeInventoryAccess;
import local.prominence.backpackinline.mixin.SlotInventoryAccess;
import net.minecraft.class_3914;

/** A delegate only: never installed as player.currentScreenHandler and never opens another UI. */
public final class InlineDelegate extends ReforgeScreenHandler {
    public InlineDelegate(InlineState state){
        super(0,state.player().method_31548(),state.player().method_37908().field_9236?class_3914.field_17304:
            class_3914.method_17392(state.player().method_37908(),state.player().method_24515()));
        ((ReforgeInventoryAccess)(Object)this).prominence$inventory(state.inventory());
        for(int i=0;i<3;i++)((SlotInventoryAccess)method_7611(i)).prominence$inventory(state.inventory());
    }
}
