package inlineselection;
import local.prominence.backpackinline.*;
import net.minecraft.*;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContainer;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.anvil.AnvilUpgradeContainer;
/** Runs the old production binary, not a rewritten approximation of its bug. */
public final class BaselineProbe {
    public static void main(String[] args)throws Exception {
        var p=new class_3222();var bag=new BackpackContainer();p.field_7512=bag;
        var anvil=new AnvilUpgradeContainer(2);bag.upgradeContainers.put(2,anvil);var state=Probe.state(anvil,p);
        new InlineReforge().onInitialize();bag.setOpenTabId(2);
        if(anvil.isOpen()||bag.getOpenContainer().orElseThrow()!=anvil)throw new AssertionError("Native reproduction failed");
        Probe.packet(p,42,2,1);
        if(state.mode())throw new AssertionError("Old binary unexpectedly accepted newly selected panel");
        anvil.setIsOpen(true);Probe.packet(p,42,2,1);
        if(!state.mode())throw new AssertionError("Old flag path not reproduced");
        bag.setOpenTabId(-1);Probe.packet(p,42,2,2);
        if(InlineDelegate.forged!=1)throw new AssertionError("Old stale-true path not reproduced");
        System.out.println("PASS: previous public production receiver reproduces rejected new-tab switch and accepted stale-open hidden-tab forge.");
    }
}
