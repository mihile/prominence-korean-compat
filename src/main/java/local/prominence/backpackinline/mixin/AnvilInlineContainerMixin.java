package local.prominence.backpackinline.mixin;

import local.prominence.backpackinline.InlineAccess;
import local.prominence.backpackinline.InlineState;
import net.minecraft.class_1657;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.anvil.AnvilUpgradeContainer;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.anvil.AnvilUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=AnvilUpgradeContainer.class,remap=false)
public abstract class AnvilInlineContainerMixin implements InlineAccess {
    @Unique private InlineState prominence$inline;
    public InlineState prominence$inline(){return prominence$inline;}
    @Inject(method="<init>",at=@At("TAIL"))
    private void prominence$init(class_1657 player,int id,AnvilUpgradeWrapper wrapper,UpgradeContainerType<?,?> type,CallbackInfo ci){
        prominence$inline=InlineState.create((AnvilUpgradeContainer)(Object)this,player);
    }
}
