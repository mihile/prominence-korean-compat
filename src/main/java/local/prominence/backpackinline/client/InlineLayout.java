package local.prominence.backpackinline.client;

import local.prominence.backpackinline.InlineReforge;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.anvil.AnvilUpgradeContainer;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.mixin.client.accessor.AbstractContainerScreenAccessor;
import net.p3pp3rf1y.sophisticatedcore.mixin.common.accessor.SlotAccessor;

public final class InlineLayout {
    private InlineLayout(){}
    public static void position(AnvilUpgradeContainer container,StorageScreenBase<?> screen,int x,int y,boolean open){
        var state=InlineReforge.state(container);if(state==null)return;
        var gui=(AbstractContainerScreenAccessor)screen;var slots=container.getSlots();
        for(int i=0;i<6;i++){
            var slot=(SlotAccessor)slots.get(i);
            if(!open||(state.mode()?i<3:i>=3)){slot.setX(-2000);continue;}
            int sx,sy;
            if(i<3){sx=i==0?4:i==1?42:80;sy=43;}
            else{sx=i==3?9:i==4?44:79;sy=i==4?34:47;}
            slot.setX(x+sx-gui.getGuiLeft());slot.setY(y+sy-gui.getGuiTop());
        }
    }
}
