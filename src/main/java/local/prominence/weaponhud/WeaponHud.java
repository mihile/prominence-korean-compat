package local.prominence.weaponhud;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.*;
import net.spell_engine.client.gui.HudRenderHelper.SpellHotBarWidget;
import net.sweenus.simplyswords.api.AwakeningApi;
import local.prominence.weaponhud.mixin.AbilityKeysAccess;

/** Display models only. Never inserts synthetic spells into the input or server spell registry. */
public final class WeaponHud {
    private WeaponHud(){}
    public static void render(class_332 draw,int width,int height,SpellHotBarWidget.ViewModel nativeView,Operation<Void> original,float delta,boolean config){
        var client=class_310.method_1551();var player=client.field_1724;
        if(config||player==null||player.method_7325()){original.call(draw,width,height,nativeView);return;}
        var extra=new ArrayList<SpellHotBarWidget.SpellViewModel>(2);
        for(var hand:class_1268.values()){
            var stack=player.method_5998(hand);boolean airborne=false;
            if(hand==class_1268.field_5808&&stack.method_7960()){
                var projectile=WeaponAbilityClient.airborne();if(projectile!=null){stack=WeaponAbilityClient.projectileStack(projectile);airborne=!stack.method_7960();}
            }
            if(!WeaponSupport.active(stack))continue;
            SpellHotBarWidget.SpellViewModel existing=null;
            for(var old:nativeView.spells())if(old.itemStack()==stack){existing=old;break;}
            var binding=hand==class_1268.field_5808?AbilityKeysAccess.prominence$main():AbilityKeysAccess.prominence$off();
            boolean rebound=!binding.method_1415()&&net.sweenus.simplyswords.client.AbilityKeybindHandler.isAbilityStack(stack)&&!WeaponSupport.defaultSpearThrow(stack);
            // Only Simply Swords reads its ability keys. Other mods retain vanilla use semantics.
            if(!class_7923.field_41178.method_10221(stack.method_7909()).method_12836().equals("simplyswords"))rebound=false;
            if(hand==class_1268.field_5810&&!rebound&&!player.method_6047().method_7960()&&existing==null)continue;
            if(!rebound)binding=client.field_1690.field_1904;
            boolean locked=stack.method_7909() instanceof net.sweenus.simplyswords.item.interfaces.UniqueWeaponActiveAbility&&!AwakeningApi.isAbilityUnlocked(stack);
            float cooldown=airborne||locked?1:player.method_7357().method_7905(stack.method_7909(),delta);
            var key=!rebound&&existing!=null?existing.keybinding():SpellHotBarWidget.KeyBindingViewModel.from(binding);
            var modifier=!rebound&&existing!=null?existing.modifier():null;
            extra.add(new SpellHotBarWidget.SpellViewModel(null,stack,cooldown,key,modifier));
        }
        if(extra.isEmpty()){original.call(draw,width,height,nativeView);return;}
        var spells=new ArrayList<SpellHotBarWidget.SpellViewModel>(nativeView.spells().size()+extra.size());spells.addAll(nativeView.spells());
        // Replace a pre-existing native item-use display for this same weapon, avoiding duplicate icons.
        for(var card:extra)spells.removeIf(old->old.itemStack()==card.itemStack());
        // Weapon cards lead the visual HUD only; native spell order and bindings stay intact.
        spells.addAll(0,extra);
        original.call(draw,width,height,new SpellHotBarWidget.ViewModel(List.copyOf(spells)));
    }
}
