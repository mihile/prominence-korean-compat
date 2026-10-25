package local.prominence.weaponhud.mixin;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import local.prominence.weaponhud.WeaponHud;
import net.minecraft.class_332;
import net.spell_engine.client.gui.HudRenderHelper;
import net.spell_engine.client.gui.HudRenderHelper.SpellHotBarWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
@Mixin(value=HudRenderHelper.class,remap=false)
public abstract class WeaponHudMixin {
    @WrapOperation(method="render(Lnet/minecraft/class_332;FZ)V",at=@At(value="INVOKE",target="Lnet/spell_engine/client/gui/HudRenderHelper$SpellHotBarWidget;render(Lnet/minecraft/class_332;IILnet/spell_engine/client/gui/HudRenderHelper$SpellHotBarWidget$ViewModel;)V"))
    private static void prominence$nativeWeaponCard(class_332 draw,int width,int height,SpellHotBarWidget.ViewModel view,Operation<Void> original,class_332 outer,float delta,boolean config){WeaponHud.render(draw,width,height,view,original,delta,config);}
}
