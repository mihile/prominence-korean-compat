package local.prominence.arrival.client;

import local.prominence.arrival.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.*;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.*;

public final class ArrivalClient implements ClientModInitializer {
    private ArrivalMessages.Begin pending;
    private class_634 connection;
    private ArrivalClientGate gate;
    private long received;
    private void clear(){pending=null;connection=null;gate=null;}
    public void onInitializeClient(){
        ClientPlayNetworking.registerGlobalReceiver(ArrivalWire.BEGIN,(client,handler,buf,sender)->{
            if(buf.readableBytes()>ArrivalMessages.MAX_BYTES)return;byte[] bytes=new byte[buf.readableBytes()];buf.readBytes(bytes);
            try{var begin=ArrivalMessages.decode(bytes);client.execute(()->{if(client.method_1562()==handler){pending=begin;connection=handler;gate=new ArrivalClientGate();received=System.nanoTime();}});}
            catch(IllegalArgumentException malformed){/* An invalid optional packet never changes gameplay state. */}
        });
        WorldRenderEvents.END.register(context->{
            var client=class_310.method_1551();if(pending==null)return;
            boolean ready=client.method_1562()==connection&&client.field_1687!=null&&client.field_1724!=null
                &&context.world()==client.field_1687&&pending.dimension().equals(client.field_1687.method_27983().method_29177().toString())
                &&client.field_1687.method_2935().method_12123(pending.chunkX(),pending.chunkZ())
                &&!(client.field_1755 instanceof class_434)&&client.method_18506()==null;
            gate.frame(ready,System.nanoTime());
        });
        ClientTickEvents.END_CLIENT_TICK.register(client->{
            if(pending==null)return;
            if(client.method_1562()!=connection||System.nanoTime()-received>=ArrivalWindow.MAX_LOADING_NANOS){clear();return;}
            if(!gate.rendered())return;
            try{if(ClientPlayNetworking.canSend(ArrivalWire.READY)){
                var buf=PacketByteBufs.create();buf.writeInt(ArrivalMessages.VERSION);buf.writeLong(pending.nonce());ClientPlayNetworking.send(ArrivalWire.READY,buf);
            }}catch(RuntimeException closing){/* The server's bounded timeout still ends protection. */}
            finally{clear();}
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->clear());
    }
}
