package local.prominence.weaponhud;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.*;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.*;
public final class WeaponHudGuard implements IMixinConfigPlugin {
    private static final class Check {static final boolean ENABLED=check();}
    private static boolean check(){
        if(System.getProperty("prominence.weaponAbilityHud","true").equalsIgnoreCase("false"))return false;
        try(var in=WeaponHudGuard.class.getResourceAsStream("/prominence-weapon-hud-native-pins.json")){
            var loader=FabricLoader.getInstance();
            if(!loader.getModContainer("minecraft").orElseThrow().getMetadata().getVersion().getFriendlyString().equals("1.20.1")||!loader.getModContainer("mixinextras").orElseThrow().getMetadata().getVersion().getFriendlyString().equals("0.5.4"))return false;
            var pins=JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonObject();
            for(var pin:pins.entrySet()){
                var mod=loader.getModContainer(pin.getKey()).orElseThrow();var expected=pin.getValue().getAsJsonObject();
                if(!mod.getMetadata().getVersion().getFriendlyString().equals(expected.get("version").getAsString()))return false;
                for(var e:expected.getAsJsonObject("classes").entrySet())if(!fingerprint(mod.findPath(e.getKey()).orElseThrow(),e.getValue().getAsString()))return false;
            }
            return true;
        }catch(Exception e){return false;}
    }
    public static boolean enabled(){return Check.ENABLED;}
    static boolean fingerprint(java.nio.file.Path path,String expected){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path))).equals(expected);}catch(Exception unavailable){return false;}}
    private static final class Extra {static final Set<String> CLASSES=readExtra();}
    public static Set<String> extraClasses(){return Extra.CLASSES;}
    private static Set<String> readExtra(){
        var valid=new HashSet<String>();
        try(var in=WeaponHudGuard.class.getResourceAsStream("/prominence-weapon-hud-extra-pins.json")){
            var pins=JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonObject();
            for(var pin:pins.entrySet())try{
                var mod=FabricLoader.getInstance().getModContainer(pin.getKey()).orElseThrow();var expected=pin.getValue().getAsJsonObject();
                if(!mod.getMetadata().getVersion().getFriendlyString().equals(expected.get("version").getAsString()))continue;
                var group=new HashSet<String>();boolean matches=true;
                for(var e:expected.getAsJsonObject("classes").entrySet()){
                    if(!fingerprint(mod.findPath(e.getKey()).orElseThrow(),e.getValue().getAsString())){matches=false;break;}
                    group.add(e.getKey().replace('/','.').replace(".class",""));
                }
                if(matches)valid.addAll(group);
            }catch(Exception unavailable){/* Optional provider: leave its display alone. */}
        }catch(Exception unavailable){return Set.of();}
        return Set.copyOf(valid);
    }
    public void onLoad(String pkg){org.slf4j.LoggerFactory.getLogger("Prominence compatibility").info("Weapon ability HUD and interaction priority: {}",enabled()?"enabled":"disabled");}
    public boolean shouldApplyMixin(String t,String m){return enabled();}
    public String getRefMapperConfig(){return null;}public List<String> getMixins(){return null;}public void acceptTargets(Set<String>a,Set<String>b){}public void preApply(String t,ClassNode n,String m,IMixinInfo i){}public void postApply(String t,ClassNode n,String m,IMixinInfo i){}
}
