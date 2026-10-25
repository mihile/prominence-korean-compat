package local.prominence.backpackinline;

import io.github.fabricators_of_create.porting_lib.transfer.item.ItemStackHandler;
import net.minecraft.class_1263;
import net.minecraft.class_1657;
import net.minecraft.class_1799;

/** Native anvil handler cells2..4; no copied item store and no global session registry. */
public final class InlineInventory implements class_1263 {
    private final InlineState state;
    InlineInventory(InlineState state){this.state=state;}
    private ItemStackHandler handler(){return state.owner().getUpgradeWrapper().getInventory();}
    public int method_5439(){return 3;}
    public boolean method_5442(){for(int i=0;i<3;i++)if(!method_5438(i).method_7960())return false;return true;}
    public class_1799 method_5438(int slot){return slot<0||slot>=3?class_1799.field_8037:handler().getStackInSlot(slot+2);}
    public class_1799 method_5434(int slot,int count){
        if(slot<0||slot>=3||count<=0)return class_1799.field_8037;
        var original=method_5438(slot);var stack=original.method_7971(count);
        if(!stack.method_7960()){handler().setStackInSlot(slot+2,original);state.changed();}return stack;
    }
    public class_1799 method_5441(int slot){return method_5434(slot,method_5438(slot).method_7947());}
    public void method_5447(int slot,class_1799 stack){if(slot<0||slot>=3)return;handler().setStackInSlot(slot+2,stack);state.changed();}
    public void method_5431(){
        // A slot or Tierify can mutate the same stack object in place. The native handler
        // serializes all five cells and invokes its original upgrade save callback.
        var h=handler();for(int i=2;i<5;i++)h.setStackInSlot(i,h.getStackInSlot(i));state.changed();
    }
    public boolean method_5443(class_1657 player){return state.player()==player;}
    public void method_5448(){for(int i=0;i<3;i++)method_5447(i,class_1799.field_8037);}
}
