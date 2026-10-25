package local.prominence.memory;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.class_1113;
import net.minecraft.class_1297;
import net.minecraft.class_1799;
import net.minecraft.class_310;
import net.minecraft.class_746;
import org.slf4j.LoggerFactory;

/** Client-only stale references; active ambience and current tooltip context remain native. */
public final class WorldReferencesClient implements ClientModInitializer {
    private final WorldReferencePruner transitions=new WorldReferencePruner();
    private boolean aquamirae,bumblezone,sortilege;
    private Field blizzardSound,blizzardActive,blizzardPlayer,flyingSounds,tooltipPlayer,tooltipStack;

    private static boolean known(String id,String version,String... pathsAndHashes) {
        if(System.getProperty("prominence.worldReferenceCleanup","true").equalsIgnoreCase("false"))return false;
        try {
            var loader=FabricLoader.getInstance();var mod=loader.getModContainer(id);
            if(!loader.getModContainer("minecraft").map(m->m.getMetadata().getVersion().getFriendlyString().equals("1.20.1")).orElse(false)
                ||mod.isEmpty()||!mod.get().getMetadata().getVersion().getFriendlyString().equals(version))return false;
            for(int i=0;i<pathsAndHashes.length;i+=2){
                var path=mod.get().findPath(pathsAndHashes[i]);
                if(path.isEmpty()||!HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path.get()))).equals(pathsAndHashes[i+1]))return false;
            }
            return true;
        }catch(Exception e){return false;}
    }
    public void onInitializeClient() {
        aquamirae=known("aquamirae","7.1.14",
            "dev/obscuria/aquamirae/client/BlizzardContext.class","78f29c7f68cc2a2dd48a1e9eeeab2b3f8070997c987062d823a75603842b7b4b",
            "dev/obscuria/aquamirae/client/sound/BlizzardSoundInstance.class","66977a3511ad8b277b9b96d2ab1f297b6981a5371df4c5bbfd45d084b7712cde");
        bumblezone=known("the_bumblezone","7.9.10+1.20.1-fabric",
            "com/telepathicgrunt/the_bumblezone/client/LivingEntityFlyingSoundInstance.class","59bb0f761a1aec6d1dff897b3a22ef5cbe2ddfe60974ccee4f6cc555f05233a8");
        sortilege=known("sortilege","9.0",
            "net/lyof/sortilege/mixin/client/ItemStackMixin.class","1941e8f572c04bfdb543584224ffabb1bf435a70d373027b2e89834382935d22");
        LoggerFactory.getLogger("Prominence compatibility").info(
            "Client obsolete world references: aquamirae={}, bumblezone={}, sortilege={}",
            aquamirae?"enabled":"disabled",bumblezone?"enabled":"disabled",sortilege?"enabled":"disabled");
        if(aquamirae||bumblezone||sortilege)ClientTickEvents.END_CLIENT_TICK.register(this::afterTick);
    }
    private static Field field(String owner,String name) throws ReflectiveOperationException {
        Class<?> type=Class.forName(owner,false,WorldReferencesClient.class.getClassLoader());
        Field field=type.getDeclaredField(name);field.setAccessible(true);return field;
    }
    private void afterTick(class_310 client) {
        if(!transitions.transition(client.field_1687,client.field_1724))return;
        int blizzard=0,flying=0,tooltip=0;
        if(aquamirae)try {
            if(blizzardSound==null){
                blizzardSound=field("dev.obscuria.aquamirae.client.BlizzardContext","sound");
                blizzardActive=field("dev.obscuria.aquamirae.client.BlizzardContext","active");
                blizzardPlayer=field("dev.obscuria.aquamirae.client.sound.BlizzardSoundInstance","player");
            }
            Object sound=blizzardSound.get(null);
            if(sound!=null){
                var owner=(class_1297)blizzardPlayer.get(sound);
                if(WorldReferencePruner.obsoleteOwner(owner,owner.method_37908(),client.field_1724,client.field_1687)){
                    blizzardSound.set(null,null);blizzardActive.setBoolean(null,false);
                    client.method_1483().method_4870((class_1113)sound);blizzard=1;
                }
            }
        }catch(ReflectiveOperationException|RuntimeException e){aquamirae=false;warn("Aquamirae",e);}
        if(bumblezone)try {
            if(flyingSounds==null)flyingSounds=field("com.telepathicgrunt.the_bumblezone.client.LivingEntityFlyingSoundInstance","activeSounds");
            @SuppressWarnings("unchecked") Map<Object,class_1113> sounds=(Map<Object,class_1113>)flyingSounds.get(null);
            flying=WorldReferencePruner.removeStale(sounds,
                entity->entity instanceof class_1297 e&&e.method_37908()==client.field_1687,
                sound->client.method_1483().method_4870(sound));
        }catch(ReflectiveOperationException|RuntimeException e){bumblezone=false;warn("Bumblezone",e);}
        if(sortilege)try {
            if(tooltipPlayer==null){tooltipPlayer=field(class_1799.class.getName(),"sorti_player");tooltipStack=field(class_1799.class.getName(),"sorti_stack");}
            Object owner=tooltipPlayer.get(null);
            // Never clear a server player or another integrated-server context.
            if(owner instanceof class_746&&owner!=client.field_1724){tooltipPlayer.set(null,null);tooltipStack.set(null,null);tooltip=1;}
        }catch(ReflectiveOperationException|RuntimeException e){sortilege=false;warn("Sortilege",e);}
        if(blizzard+flying+tooltip>0)LoggerFactory.getLogger("Prominence compatibility").info(
            "Client obsolete world references released: blizzard={}, flying={}, tooltip={}",blizzard,flying,tooltip);
    }
    private static void warn(String mod,Exception error) {
        LoggerFactory.getLogger("Prominence compatibility").warn("Disabling obsolete reference cleanup for {}; retaining native behavior",mod,error);
    }
}
