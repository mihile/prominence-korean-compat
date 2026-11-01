package local.prominence.cinder;

import java.nio.file.*;
import java.util.Properties;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.*;
import net.minecraft.server.MinecraftServer;
import static com.mojang.brigadier.arguments.IntegerArgumentType.*;
import static net.minecraft.class_2170.*;

public final class CinderRewards implements ModInitializer {
    public static final class_2960 SYNC = new class_2960("prominence_korean_compat", "cinder_multiplier");
    private static MinecraftServer server;
    private static int multiplier = 1;
    private static Path file(){return FabricLoader.getInstance().getConfigDir().resolve("prominence-cinder.properties");}
    public static int multiplier(){return multiplier;}
    public static int scaled(int base,int factor){return CinderMath.scale(base,factor);}
    public static void sync(int factor,java.util.Set<class_3222> sent){
        if(server==null)return;
        for(var player:server.method_3760().method_14571())if(!sent.contains(player)&&ServerPlayNetworking.canSend(player,SYNC)){
            var buf=PacketByteBufs.create();buf.writeInt(factor);ServerPlayNetworking.send(player,SYNC,buf);
            sent.add(player);
        }
    }
    public void onInitialize(){
        ServerLifecycleEvents.SERVER_STARTED.register(s->{server=s;multiplier=1;
            if(Files.exists(file()))try(var in=Files.newInputStream(file())){var p=new Properties();p.load(in);int n=Integer.parseInt(p.getProperty("multiplier","1"));if(n<1||n>1000)throw new IllegalArgumentException();multiplier=n;}
            catch(Exception e){org.slf4j.LoggerFactory.getLogger("Cinder rewards").warn("Invalid cinder multiplier; using 1",e);}
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(s->{if(server==s){server=null;multiplier=1;}});
        CommandRegistrationCallback.EVENT.register((dispatcher,access,environment)->dispatcher.register(
            method_9247("prominence").then(method_9247("cinder").requires(source->source.method_9259(2))
                .executes(ctx->{ctx.getSource().method_9226(()->class_2561.method_43470("잿불 코인 보상 배수: ×"+multiplier),false);return multiplier;})
                .then(method_9244("multiplier",integer(1,1000)).executes(ctx->{
                    int n=getInteger(ctx,"multiplier");
                    try{var p=new Properties();p.setProperty("multiplier",Integer.toString(n));Path temp=file().resolveSibling("prominence-cinder.properties.tmp");try(var out=Files.newOutputStream(temp)){p.store(out,"Hourglass raid reward multiplier");}Files.move(temp,file(),StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}
                    catch(Exception e){ctx.getSource().method_9213(class_2561.method_43470("배수 저장에 실패했습니다. 기존 배수를 유지합니다."));return 0;}
                    multiplier=n;ctx.getSource().method_9226(()->class_2561.method_43470("모래시계 레이드 잿불 코인 보상: ×"+n),true);return n;
                })))));
    }
}
