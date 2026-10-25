package local.prominence.backpackinline.client;

import io.netty.buffer.Unpooled;
import local.prominence.backpackinline.InlineGuard;
import local.prominence.backpackinline.InlineReforge;
import local.prominence.backpackinline.InlineState;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.class_2540;
import net.minecraft.class_310;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContainer;

public final class InlineReforgeClient implements ClientModInitializer {
    private static boolean agreed;
    private static int helloAttempts;
    private static long nextHello;
    public static boolean agreed(){return agreed;}
    public static boolean advertised(){return ClientPlayNetworking.canSend(InlineReforge.CHANNEL);}
    public void onInitializeClient(){
        if(!InlineGuard.enabled())return;
        InlineReforge.clientCapability(p->agreed&&ClientPlayNetworking.canSend(InlineReforge.CHANNEL));
        ClientPlayConnectionEvents.JOIN.register((network,sender,client)->{agreed=false;helloAttempts=0;nextHello=0;});
        ClientPlayConnectionEvents.DISCONNECT.register((network,client)->{agreed=false;helloAttempts=0;nextHello=0;});
        ClientTickEvents.END_CLIENT_TICK.register(client->{
            if(agreed||client.field_1724==null||client.field_1724.field_7512 instanceof BackpackContainer||helloAttempts>=10||!ClientPlayNetworking.canSend(InlineReforge.CHANNEL))return;
            long now=System.nanoTime();if(nextHello!=0&&now-nextHello<0)return;
            helloAttempts++;nextHello=now+1_000_000_000L;
            var data=new class_2540(Unpooled.buffer());data.writeInt(-1);data.writeInt(1);data.writeInt(4);ClientPlayNetworking.send(InlineReforge.CHANNEL,data);
        });
        ClientPlayNetworking.registerGlobalReceiver(InlineReforge.CHANNEL,(client,network,buffer,sender)->{
            if(buffer.readableBytes()!=10)return;int sync=buffer.readInt(),id=buffer.readInt();boolean mode=buffer.readBoolean(),ready=buffer.readBoolean();
            client.execute(()->{
                if(client.method_1562()!=network)return; // A queued reply from a previous connection is obsolete.
                if(sync==-1&&id==1){agreed=true;return;}
                if(client.field_1724==null||!(client.field_1724.field_7512 instanceof BackpackContainer bag)||bag.field_7763!=sync)return;
                InlineState state=InlineReforge.state(bag.getUpgradeContainers().get(id));if(state!=null)state.remote(mode,ready);
            });
        });
    }
    public static InlineState state(int id){
        var player=class_310.method_1551().field_1724;
        return player!=null&&player.field_7512 instanceof BackpackContainer bag?InlineReforge.state(bag.getUpgradeContainers().get(id)):null;
    }
    public static void send(int id,int action){
        var player=class_310.method_1551().field_1724;
        if(player==null||!ClientPlayNetworking.canSend(InlineReforge.CHANNEL))return;
        var data=new class_2540(Unpooled.buffer());data.writeInt(player.field_7512.field_7763);data.writeInt(id);data.writeInt(action);
        ClientPlayNetworking.send(InlineReforge.CHANNEL,data);
    }
}
