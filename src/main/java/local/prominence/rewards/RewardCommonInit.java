package local.prominence.rewards;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.S2CPlayChannelEvents;
/** Handles clients which advertise the display channel after the initial data sync. */
public final class RewardCommonInit implements ModInitializer {
 public void onInitialize(){S2CPlayChannelEvents.REGISTER.register((handler,sender,server,channels)->{if(channels.contains(RewardWire.CHANNEL))server.execute(()->RewardWire.send(handler.field_14140));});}
}
