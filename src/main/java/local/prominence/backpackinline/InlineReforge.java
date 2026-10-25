package local.prominence.backpackinline;

import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.function.Predicate;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.class_1657;
import net.minecraft.class_1735;
import net.minecraft.class_2540;
import net.minecraft.class_2960;
import net.minecraft.class_3222;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContainer;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.anvil.AnvilUpgradeContainer;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerBase;

public final class InlineReforge implements ModInitializer {
    public static final class_2960 CHANNEL=new class_2960("prominence_korean_compat","inline_reforge_v1");
    private static Predicate<class_1657> clientCapability=p->false;
    public static void clientCapability(Predicate<class_1657> capability){clientCapability=capability;}
    public static boolean available(class_1657 player){
        return InlineGuard.enabled()&&(player instanceof class_3222 server?ServerPlayNetworking.canSend(server,CHANNEL)
            &&((HandshakeAccess)(Object)server.field_13987).prominence$inlineAgreed():clientCapability.test(player));
    }
    public void onInitialize(){
        if(!InlineGuard.enabled())return;
        ServerPlayNetworking.registerGlobalReceiver(CHANNEL,(server,player,network,buffer,sender)->{
            if(buffer.readableBytes()!=12)return;int sync=buffer.readInt(),id=buffer.readInt(),action=buffer.readInt();
            if(action<0||action>4)return;
            server.execute(()->{
                if(action==4){
                    // Never change the layout agreement while a legacy-layout bag is still open.
                    if(sync!=-1||id!=1||player.field_7512 instanceof BackpackContainer||!ServerPlayNetworking.canSend(player,CHANNEL))return;
                    ((HandshakeAccess)(Object)player.field_13987).prominence$inlineAgree();
                    var ack=new class_2540(Unpooled.buffer());ack.writeInt(-1);ack.writeInt(1);ack.writeBoolean(false);ack.writeBoolean(false);
                    // Sent on the server thread before any later six-slot backpack open packet.
                    ServerPlayNetworking.send(player,CHANNEL,ack);return;
                }
                if(!available(player)||!(player.field_7512 instanceof BackpackContainer bag)||bag.field_7763!=sync||!bag.method_7597(player))return;
                var container=bag.getUpgradeContainers().get(id);
                if(!(container instanceof AnvilUpgradeContainer)||!container.isOpen())return;
                InlineState state=state(container);if(state==null||state.player()!=player)return;
                state.action(action);
            });
        });
    }
    public static InlineState state(Object container){return container instanceof InlineAccess a?a.prominence$inline():null;}
    public static void sync(InlineState state){
        if(!(state.player() instanceof class_3222 player)||!(player.field_7512 instanceof BackpackContainer bag)
            ||bag.getUpgradeContainers().get(state.owner().getUpgradeContainerId())!=state.owner()||!ServerPlayNetworking.canSend(player,CHANNEL))return;
        var data=new class_2540(Unpooled.buffer());data.writeInt(bag.field_7763);data.writeInt(state.owner().getUpgradeContainerId());data.writeBoolean(state.mode());data.writeBoolean(state.ready());
        ServerPlayNetworking.send(player,CHANNEL,data);
    }
    public static List<class_1735> transferSlots(UpgradeContainerBase<?,?> container){
        InlineState s=state(container);return s==null?container.getSlots():s.activeSlots();
    }
    public static boolean blocked(Object menu,class_1735 slot){
        if(!(menu instanceof StorageContainerMenuBase<?> bag))return false;
        for(var upgrade:bag.getUpgradeContainers().values()){
            InlineState s=state(upgrade);if(s!=null&&upgrade.containsSlot(slot))return !s.slotActive(slot);
        }
        return false;
    }
    public static boolean blocked(Object menu,int slot){
        if(!(menu instanceof StorageContainerMenuBase<?> bag)||slot<0||slot>=bag.getTotalSlotsNumber())return false;
        return blocked(menu,bag.method_7611(slot));
    }
    public static void poll(Object menu){
        if(menu instanceof StorageContainerMenuBase<?> bag)for(var upgrade:bag.getUpgradeContainers().values()){
            InlineState s=state(upgrade);if(s!=null)s.poll();
        }
    }
}
