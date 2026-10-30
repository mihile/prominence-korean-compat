package local.prominence.arrival;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.networking.v1.*;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.*;
import net.minecraft.server.MinecraftServer;

/** Damage protection belongs to the server; no abilities, status effects or saved player data are edited. */
public final class ArrivalServer implements ModInitializer {
    private record State(ArrivalWindow window,String dimension){}
    public record Attempt(class_3222 player,ArrivalWindow window,Object previous){}
    private static final Map<class_3222,State> WINDOWS=new WeakHashMap<>();
    public static Attempt begin(class_3222 player){
        synchronized(WINDOWS){
            var window=new ArrivalWindow(ThreadLocalRandom.current().nextLong(),System.nanoTime());
            State previous=WINDOWS.put(player,new State(window,null));return new Attempt(player,window,previous);
        }
    }
    public static void abort(Attempt attempt){
        synchronized(WINDOWS){
            State state=WINDOWS.get(attempt.player());if(state==null||state.window()!=attempt.window())return;
            State previous=(State)attempt.previous();
            if(previous!=null&&previous.window().active(attempt.player().field_13995.method_3780(),System.nanoTime()))WINDOWS.put(attempt.player(),previous);
            else WINDOWS.remove(attempt.player());
        }
    }
    public static void arrived(Attempt attempt,class_3218 destination){
        class_3222 player=attempt.player();
        if(player.method_37908()!=destination||player.method_31481()){abort(attempt);return;}
        boolean peer;
        try{peer=ServerPlayNetworking.canSend(player,ArrivalWire.BEGIN);}catch(RuntimeException unsupported){peer=false;}
        String dimension=destination.method_27983().method_29177().toString();
        synchronized(WINDOWS){
            State state=WINDOWS.get(player);if(state==null||state.window()!=attempt.window())return;
            state.window().arrived(peer,player.field_13995.method_3780());WINDOWS.put(player,new State(state.window(),dimension));
        }
        if(peer)try{
            var message=new ArrivalMessages.Begin(attempt.window().nonce,dimension,((int)Math.floor(player.method_23317()))>>4,((int)Math.floor(player.method_23321()))>>4);
            var buf=PacketByteBufs.create();buf.writeBytes(ArrivalMessages.encode(message));ServerPlayNetworking.send(player,ArrivalWire.BEGIN,buf);
        }catch(RuntimeException failure){
            synchronized(WINDOWS){State state=WINDOWS.get(player);if(state!=null&&state.window()==attempt.window())state.window().legacy(player.field_13995.method_3780());}
            org.slf4j.LoggerFactory.getLogger("Prominence compatibility").warn("Waystone arrival sync failed; using bounded legacy protection",failure);
        }
    }
    public static boolean protectedNow(class_3222 player){
        synchronized(WINDOWS){
            State state=WINDOWS.get(player);if(state==null)return false;
            if(player.method_31481()||!player.method_5805()||state.dimension()!=null&&!state.dimension().equals(player.method_37908().method_27983().method_29177().toString())||!state.window().active(player.field_13995.method_3780(),System.nanoTime())){WINDOWS.remove(player);return false;}
            return true;
        }
    }
    private static void ready(class_3222 player,long nonce){
        synchronized(WINDOWS){State state=WINDOWS.get(player);if(state!=null&&protectedNow(player))state.window().ready(nonce,player.field_13995.method_3780(),System.nanoTime());}
    }
    private static void clean(MinecraftServer server){
        synchronized(WINDOWS){var it=WINDOWS.entrySet().iterator();while(it.hasNext()){
            var e=it.next();class_3222 player=e.getKey();if(player==null){it.remove();continue;}
            if(player.field_13995==server&&(player.method_31481()||!player.method_5805()||!e.getValue().window().active(server.method_3780(),System.nanoTime())))it.remove();
        }}
    }
    public void onInitialize(){
        ServerPlayNetworking.registerGlobalReceiver(ArrivalWire.READY,(server,player,handler,buf,sender)->{
            if(buf.readableBytes()!=12||buf.readInt()!=ArrivalMessages.VERSION)return;long nonce=buf.readLong();
            server.execute(()->{if(player.field_13987==handler)ready(player,nonce);});
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler,server)->{synchronized(WINDOWS){WINDOWS.remove(handler.field_14140);}});
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer,newPlayer,alive)->{synchronized(WINDOWS){WINDOWS.remove(oldPlayer);WINDOWS.remove(newPlayer);}});
        ServerTickEvents.END_SERVER_TICK.register(ArrivalServer::clean);
        ServerLifecycleEvents.SERVER_STOPPED.register(server->{synchronized(WINDOWS){WINDOWS.keySet().removeIf(player->player==null||player.field_13995==server);}});
    }
}
