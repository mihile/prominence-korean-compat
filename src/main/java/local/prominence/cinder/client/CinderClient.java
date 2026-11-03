package local.prominence.cinder.client;
import local.prominence.cinder.CinderRewards;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.*;
public final class CinderClient implements ClientModInitializer {
    public static int multiplier=1;
    public void onInitializeClient(){
        ClientPlayNetworking.registerGlobalReceiver(CinderRewards.SYNC,(client,handler,buf,sender)->{
            if(buf.readableBytes()!=4)return;int n=buf.readInt();if(n<1||n>1000)return;
            client.execute(()->{if(client.method_1562()==handler)multiplier=n;});
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->multiplier=1);
    }
}
