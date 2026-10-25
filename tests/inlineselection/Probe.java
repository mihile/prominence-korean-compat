package inlineselection;
import java.lang.reflect.*;
import local.prominence.backpackinline.*;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.*;
import net.minecraft.server.MinecraftServer;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContainer;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.anvil.AnvilUpgradeContainer;
public final class Probe {
    static int checks;
    static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    static boolean opened(AnvilUpgradeContainer c,class_1657 p)throws Exception {
        return (boolean)InlineReforge.class.getMethod("isOpen",net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerBase.class,class_1657.class).invoke(null,c,p);
    }
    static InlineState state(AnvilUpgradeContainer c,class_1657 p)throws Exception {
        var constructor=InlineState.class.getDeclaredConstructor(AnvilUpgradeContainer.class,class_1657.class);constructor.setAccessible(true);
        return c.state=constructor.newInstance(c,p);
    }
    static void packet(class_3222 p,int sync,int id,int action){
        var data=new class_2540(io.netty.buffer.Unpooled.buffer());data.writeInt(sync);data.writeInt(id);data.writeInt(action);
        try{ServerPlayNetworking.receiver.receive(new MinecraftServer(),p,p.field_13987,data,null);}finally{data.release();}
    }
    public static void main(String[] args)throws Exception {
        var p=new class_3222();var bag=new BackpackContainer();p.field_7512=bag;
        var anvil=new AnvilUpgradeContainer(2);var other=new AnvilUpgradeContainer(3);
        bag.upgradeContainers.put(2,anvil);bag.upgradeContainers.put(3,other);var state=state(anvil,p);var otherState=state(other,p);
        new InlineReforge().onInitialize();
        check(!opened(anvil,p),"Initial closed panel");
        bag.setOpenTabId(2);
        check(!anvil.isOpen(),"Original Core reproduces stale false server flag after selecting tab");
        check(bag.getOpenContainer().orElseThrow()==anvil,"Native wrapper selects actual anvil");
        check(opened(anvil,p),"Fixed selection recognizes newly opened tab");
        InlineReforge.poll(bag);check(InlineDelegate.refreshed==1,"Newly selected tab refreshes readiness");
        int sent=ServerPlayNetworking.sent;
        packet(p,42,2,1);check(state.mode(),"Production receiver switches to inline reforge despite false flag");
        check(ServerPlayNetworking.sent>sent&&ServerPlayNetworking.lastMode&&ServerPlayNetworking.lastReady,"Actual production sync returns new mode and readiness");
        check(!InlineReforge.blocked(bag,anvil.getSlots().get(3)),"Visible reforge input enabled");
        check(InlineReforge.blocked(bag,anvil.getSlots().get(0)),"Hidden anvil input blocked");
        packet(p,42,2,2);check(InlineDelegate.forged==1,"Production receiver allows forge on selected tab");
        packet(p,42,2,2);check(InlineDelegate.forged==1,"Same-tick duplicate forge rejected");
        p.world.tick++;packet(p,42,2,2);check(InlineDelegate.forged==2,"Next-tick forge allowed");
        packet(p,42,2,0);check(!state.mode(),"Return to anvil on same panel");
        check(!InlineReforge.blocked(bag,anvil.getSlots().get(0)),"Anvil input enabled after return");
        packet(p,42,2,1);anvil.setIsOpen(true);bag.setOpenTabId(3);
        check(anvil.isOpen(),"Original flag remains true after native switch-away");
        check(!opened(anvil,p),"Stale true flag cannot authorize inactive tab");
        check(InlineReforge.blocked(bag,anvil.getSlots().get(3)),"Old reforge slot blocked after switch-away");
        p.world.tick++;packet(p,42,2,2);check(InlineDelegate.forged==2,"Hidden-tab forge packet rejected");
        packet(p,42,2,0);check(state.mode(),"Hidden-tab mode packet rejected");
        bag.setOpenTabId(-1);check(!opened(other,p),"Closed native wrapper has no active panel");
        bag.setOpenTabId(2);bag.accessible=false;packet(p,42,2,0);check(state.mode(),"Inaccessible bag rejected");bag.accessible=true;
        packet(p,43,2,0);check(state.mode(),"Stale sync ID rejected");
        packet(p,42,99,0);check(state.mode(),"Unknown upgrade ID rejected");
        p.field_13987.agreed=false;packet(p,42,2,0);check(state.mode(),"Unnegotiated peer rejected");p.field_13987.agreed=true;
        ServerPlayNetworking.supported=false;packet(p,42,2,0);check(state.mode(),"Unsupported peer rejected");ServerPlayNetworking.supported=true;
        bag.upgradeContainers.put(2,new AnvilUpgradeContainer(2));check(!opened(anvil,p),"Replaced owner with same ID rejected");bag.upgradeContainers.put(2,anvil);
        p.field_7512=new BackpackContainer();check(!opened(anvil,p),"Old menu owner rejected after menu replacement");p.field_7512=bag;
        bag.setOpenTabId(3);int refreshes=InlineDelegate.refreshed;state.poll();check(InlineDelegate.refreshed==refreshes,"Hidden panel not polled despite stale true flag");
        state.action(0);check(state.mode(),"Direct state action also rejects hidden panel");
        bag.setOpenTabId(2);p.world.field_9236=true;state.action(0);check(state.mode(),"Client cannot perform server action");p.world.field_9236=false;
        packet(p,42,2,0);check(!state.mode(),"Native reopen resumes working without new screen");
        check(anvil.getSlots().size()==6&&other.getSlots().size()==6,"Slot registration counts unchanged");
        check(!otherState.mode(),"Other upgrade mode untouched");
        System.out.println("PASS: "+checks+" production inline receiver/state selection checks, stale-false reproduction and stale-true safety.");
    }
}
