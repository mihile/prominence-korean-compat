package local.prominence.weaponhud;

import java.lang.ref.WeakReference;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.*;
import net.sweenus.simplyswords.entity.ThrownSwordEntity;
import net.spell_engine.mixin.client.control.KeybindingAccessor;
import local.prominence.weaponhud.mixin.ProjectileStackAccess;

public final class WeaponAbilityClient implements ClientModInitializer {
    private static final UsePressFence FENCE=new UsePressFence();
    private static WeakReference<ThrownSwordEntity> thrown=new WeakReference<>(null);
    public void onInitializeClient(){
        if(!WeaponHudGuard.enabled())return;
        ClientTickEvents.START_CLIENT_TICK.register(client->{
            if(client.field_1724==null){reset();return;}
            FENCE.observe(client.field_1690.field_1904.method_1434(),client.field_1755!=null);
        });
        ClientPlayConnectionEvents.DISCONNECT.register((network,client)->reset());
        ClientPlayConnectionEvents.JOIN.register((network,sender,client)->reset());
        ClientEntityEvents.ENTITY_LOAD.register((entity,world)->{
            var player=class_310.method_1551().field_1724;
            if(entity instanceof ThrownSwordEntity projectile&&player!=null&&projectile.method_24921()==player)thrown=new WeakReference<>(projectile);
        });
        ClientEntityEvents.ENTITY_UNLOAD.register((entity,world)->{if(thrown.get()==entity)thrown.clear();});
    }
    private static void reset(){FENCE.reset();thrown.clear();}
    public static void interacted(class_1269 result){if(result!=class_1269.field_5811)FENCE.consume();}
    public static boolean blocked(class_304 binding){
        var client=class_310.method_1551();if(client.field_1724==null||client.field_1687==null)return false;
        var use=client.field_1690.field_1904;boolean same=binding==use||((KeybindingAccessor)binding).getBoundKey().equals(((KeybindingAccessor)use).getBoundKey());
        if(!same)return false;
        boolean container=false;
        if(client.field_1765 instanceof class_3965 hit&&hit.method_17783().name().equals("BLOCK")&&!client.field_1724.method_5715()){
            var pos=hit.method_17777();container=client.field_1687.method_8320(pos).method_26196(client.field_1687,pos)!=null;
        }
        boolean mining=client.field_1761!=null&&client.field_1761.method_2923();
        if(container||mining)FENCE.consume();
        return FENCE.blocks(true,use.method_1434()||container||mining,container,client.field_1724.method_5715());
    }
    public static boolean blockedDefault(class_1657 player,class_1268 hand){
        return player==class_310.method_1551().field_1724&&WeaponSupport.weapon(player.method_5998(hand))&&blocked(class_310.method_1551().field_1690.field_1904);
    }
    public static ThrownSwordEntity airborne(){
        var entity=thrown.get();var client=class_310.method_1551();
        return entity!=null&&!entity.method_31481()&&entity.method_37908()==client.field_1687&&entity.method_24921()==client.field_1724?entity:null;
    }
    public static class_1799 projectileStack(ThrownSwordEntity entity){
        // Livyatan overrides getItemStack() with a server-side field. Read the synced tracker instead.
        return entity.method_5841().method_12789(ProjectileStackAccess.prominence$stackKey());
    }
}
