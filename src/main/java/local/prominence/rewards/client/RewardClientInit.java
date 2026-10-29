package local.prominence.rewards.client;
import net.fabricmc.api.ClientModInitializer;import net.fabricmc.fabric.api.client.networking.v1.*;import local.prominence.rewards.RewardWire;
public final class RewardClientInit implements ClientModInitializer {
 private final RewardPollCache<Object> packets=new RewardPollCache<>();
 public void onInitializeClient(){
  ClientPlayNetworking.registerGlobalReceiver(RewardWire.CHANNEL,(client,handler,buf,sender)->{
   try{if(buf.readableBytes()>RewardWire.MAX_BYTES)throw new IllegalArgumentException("Reward packet size");var bytes=new byte[buf.readableBytes()];buf.readBytes(bytes);if(packets.duplicate(handler,bytes))return;var definitions=RewardWire.decode(bytes);packets.accepted(handler,bytes);client.execute(()->{if(client.method_1562()==handler)RewardClientState.install(definitions);});}
   catch(RuntimeException e){client.execute(()->{if(client.method_1562()==handler){packets.clear();RewardClientState.clear();}});org.slf4j.LoggerFactory.getLogger("Prominence compatibility").warn("Invalid reward metadata; keep native loot display");}
  });
  ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->{packets.clear();RewardClientState.clear();RewardClientText.reset();});
 }
}
