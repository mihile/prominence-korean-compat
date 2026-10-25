package local.prominence.backpackinline.mixin;

import local.prominence.backpackcompat.client.PortableReforgeButton;
import local.prominence.backpackinline.InlineReforge;
import local.prominence.backpackinline.client.InlineLayout;
import local.prominence.backpackinline.client.InlinePanel;
import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_332;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.anvil.AnvilUpgradeContainer;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.anvil.AnvilUpgradeTab;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.UpgradeSettingsTab;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.TextBox;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Dimension;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=AnvilUpgradeTab.class,remap=false,priority=500)
public abstract class InlineAnvilTabMixin extends UpgradeSettingsTab<AnvilUpgradeContainer> {
    @Shadow @Final private TextBox itemNameTextBox;
    protected InlineAnvilTabMixin(AnvilUpgradeContainer c,Position p,StorageScreenBase<?> s,class_2561 title,class_2561 tip){super(c,p,s,title,tip);}
    @Inject(method="<init>",at=@At("TAIL"))
    private void prominence$controls(AnvilUpgradeContainer c,Position p,StorageScreenBase<?> s,CallbackInfo ci){
        openTabDimension=new Dimension(103,116);
        addHideableChild(new PortableReforgeButton(new Position(x+3,y+92),c.getUpgradeContainerId(),true));
        addHideableChild(new InlinePanel(new Position(x+3,y+24),c.getUpgradeContainerId()));
    }
    @Inject(method="renderBg",at=@At("HEAD"))
    private void prominence$layout(class_332 draw,class_310 client,int mx,int my,CallbackInfo ci){
        var s=InlineReforge.state(getContainer());boolean reforge=s!=null&&s.mode();
        itemNameTextBox.setVisible(isOpen&&!reforge);if(reforge)itemNameTextBox.method_25365(false);
        InlineLayout.position(getContainer(),screen,x,y,isOpen);
    }
    @Inject(method="moveSlotsToTab",at=@At("TAIL"))
    private void prominence$move(CallbackInfo ci){InlineLayout.position(getContainer(),screen,x,y,isOpen);}
    @Inject(method="renderCost",at=@At("HEAD"),cancellable=true)
    private void prominence$hideAnvilCost(class_332 draw,int x,int y,CallbackInfo ci){var s=InlineReforge.state(getContainer());if(s!=null&&s.mode())ci.cancel();}
}
