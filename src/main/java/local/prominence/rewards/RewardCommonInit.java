package local.prominence.rewards;
import java.util.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.class_3244;
import net.minecraft.server.MinecraftServer;
/** REGISTER may run inside the network addon's constructor. Only queue there. */
public final class RewardCommonInit implements ModInitializer {
 private static final Map<class_3244,Boolean> PENDING=new WeakHashMap<>();
 public static void request(class_3244 handler){if(handler==null)return;synchronized(PENDING){PENDING.put(handler,Boolean.TRUE);}}
 private static void forget(class_3244 handler){synchronized(PENDING){PENDING.remove(handler);}}
 private static void flush(MinecraftServer server){
  var due=new ArrayList<class_3244>();
  synchronized(PENDING){var it=PENDING.keySet().iterator();while(it.hasNext()){var handler=it.next();if(handler!=null&&handler.field_14140.field_13995==server){due.add(handler);it.remove();}}}
  for(var handler:due)if(handler.field_14140.field_13987==handler)RewardWire.send(handler.field_14140);
 }
 public void onInitialize(){
  S2CPlayChannelEvents.REGISTER.register((handler,sender,server,channels)->{if(channels.contains(RewardWire.CHANNEL))request(handler);});
  ServerPlayConnectionEvents.JOIN.register((handler,sender,server)->request(handler));
  ServerPlayConnectionEvents.DISCONNECT.register((handler,server)->forget(handler));
  ServerTickEvents.END_SERVER_TICK.register(RewardCommonInit::flush);
 }
}
