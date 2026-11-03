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
 private static final Map<class_3244,Long> SENT=new WeakHashMap<>();
 public static boolean shouldQueue(Object key,Map<Object,Long> sent,long epoch){
  return key!=null&&(epoch<=0||!Objects.equals(sent.get(key),epoch));
 }
 public static void request(class_3244 handler){
  if(handler==null)return;
  long ep=RewardRegistry.epoch();
  synchronized(PENDING){
   if(ep>0&&Objects.equals(SENT.get(handler),ep))return;
   PENDING.put(handler,Boolean.TRUE);
  }
 }
 private static void forget(class_3244 handler){synchronized(PENDING){PENDING.remove(handler);SENT.remove(handler);}}
 private static void flush(MinecraftServer server){
  var due=new ArrayList<class_3244>();
  long ep=RewardRegistry.epoch();
  synchronized(PENDING){var it=PENDING.keySet().iterator();while(it.hasNext()){var handler=it.next();if(handler!=null&&handler.field_14140.field_13995==server){if(ep<=0||!Objects.equals(SENT.get(handler),ep))due.add(handler);it.remove();}}}
  for(var handler:due)if(handler.field_14140.field_13987==handler&&RewardWire.send(handler.field_14140)){
   long sentEpoch=RewardRegistry.epoch();
   if(sentEpoch>0)synchronized(PENDING){SENT.put(handler,sentEpoch);}
  }
 }
 public void onInitialize(){
  S2CPlayChannelEvents.REGISTER.register((handler,sender,server,channels)->{if(channels.contains(RewardWire.CHANNEL)||channels.contains(RewardWire.DISPLAY_CHANNEL))request(handler);});
  ServerPlayConnectionEvents.JOIN.register((handler,sender,server)->request(handler));
  ServerPlayConnectionEvents.DISCONNECT.register((handler,server)->forget(handler));
  ServerTickEvents.END_SERVER_TICK.register(RewardCommonInit::flush);
 }
}
