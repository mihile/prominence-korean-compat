package local.prominence.backpackinline;

import elocindev.tierify.screen.ReforgeScreenHandler;
import java.util.ArrayList;
import java.util.List;
import local.prominence.backpackinline.mixin.ReforgeInventoryAccess;
import net.minecraft.class_1657;
import net.minecraft.class_1735;
import net.minecraft.class_1799;
import net.minecraft.class_2487;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.anvil.AnvilUpgradeContainer;

public final class InlineState {
    private final AnvilUpgradeContainer owner;
    private final class_1657 player;
    private final InlineInventory inventory;
    private final InlineDelegate delegate;
    private boolean mode,ready,batching;
    private class_2487 lastSaved;
    private long lastForge=Long.MIN_VALUE;
    private InlineState(AnvilUpgradeContainer owner,class_1657 player){
        this.owner=owner;this.player=player;inventory=new InlineInventory(this);delegate=new InlineDelegate(this);
        for(int i=0;i<3;i++)owner.getSlots().add(delegate.method_7611(i));
    }
    public static InlineState create(AnvilUpgradeContainer owner,class_1657 player){
        if(!InlineReforge.available(player))return null;
        var h=owner.getUpgradeWrapper().getInventory();int size=h.getSlotCount();
        if(size!=2&&size!=5)return null; // Preserve unknown inventory schemas untouched.
        if(size==2){
            var a=h.getStackInSlot(0);var b=h.getStackInSlot(1);h.setSize(5);h.setStackInSlot(0,a);h.setStackInSlot(1,b);
        }
        return new InlineState(owner,player);
    }
    public AnvilUpgradeContainer owner(){return owner;}
    public class_1657 player(){return player;}
    public InlineInventory inventory(){return inventory;}
    public boolean mode(){return mode;}
    public boolean ready(){return ready;}
    public void remote(boolean mode,boolean ready){if(player.method_37908().field_9236){this.mode=mode;this.ready=ready;}}
    public List<class_1735> activeSlots(){return owner.getSlots().subList(mode?3:0,mode?6:3);}
    public boolean slotActive(class_1735 slot){return InlineReforge.isOpen(owner,player)&&activeSlots().contains(slot);}
    private class_2487 saved(){return owner.getUpgradeStack().method_7948().method_10562("craftingInventory");}
    public void changed(){if(!batching&&!player.method_37908().field_9236)refresh();}
    private void refresh(){
        delegate.method_7609(inventory);
        ready=((ReforgeInventoryAccess)(Object)delegate).prominence$ready()&&ReforgeScreenHandler.isValidAddition(inventory.method_5438(2));
        lastSaved=saved();InlineReforge.sync(this);
    }
    public void poll(){if(InlineReforge.isOpen(owner,player)&&!player.method_37908().field_9236&&lastSaved!=saved())refresh();}
    public void action(int action){
        if(player.method_37908().field_9236||!InlineReforge.isOpen(owner,player))return;
        if(action==0||action==1){mode=action==1;refresh();return;}
        if(action==3){refresh();return;}
        if(action!=2||!mode)return;
        refresh();long tick=player.method_37908().method_8510();
        if(!ready||tick==lastForge)return;
        lastForge=tick;batching=true;
        try{delegate.reforge();}finally{batching=false;inventory.method_5431();}
    }
}
