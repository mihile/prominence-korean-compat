package local.prominence.weaponhud;
import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipFile;
public final class WeaponHudPinChecks {
    public static void main(String[]args)throws Exception{
        var pins=new JsonObject();
        for(String file:List.of("/prominence-weapon-hud-native-pins.json","/prominence-weapon-hud-extra-pins.json"))try(var in=WeaponHudPinChecks.class.getResourceAsStream(file)){JsonParser.parseString(new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject().entrySet().forEach(e->pins.add(e.getKey(),e.getValue()));}
        var sources=new HashMap<String,Path>();try(var files=Files.list(Path.of(args[0]))){for(var jar:files.filter(p->p.toString().endsWith(".jar")).toList())try(var z=new ZipFile(jar.toFile())){var e=z.getEntry("fabric.mod.json");if(e==null)continue;var meta=JsonParser.parseString(new String(z.getInputStream(e).readAllBytes(),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();if(pins.has(meta.get("id").getAsString()))sources.put(meta.get("id").getAsString(),jar);}}
        int checks=0;
        for(var provider:pins.entrySet())try(var fs=FileSystems.newFileSystem(sources.get(provider.getKey()),Map.of())){
            var expected=provider.getValue().getAsJsonObject();var meta=JsonParser.parseString(Files.readString(fs.getPath("/fabric.mod.json"))).getAsJsonObject();if(!meta.get("version").equals(expected.get("version")))throw new AssertionError("Version differs");checks++;
            for(var entry:expected.getAsJsonObject("classes").entrySet()){
                var path=fs.getPath("/"+entry.getKey());if(!WeaponHudGuard.fingerprint(path,entry.getValue().getAsString()))throw new AssertionError("Native class rejected: "+path);checks++;
                if(WeaponHudGuard.fingerprint(path,"0".repeat(64))||WeaponHudGuard.fingerprint(fs.getPath("/missing/class"),entry.getValue().getAsString()))throw new AssertionError("Modified/missing class accepted");checks+=2;
            }
        }
        String previous=System.getProperty("prominence.weaponAbilityHud");try{System.setProperty("prominence.weaponAbilityHud","false");if(new WeaponHudGuard().shouldApplyMixin("target","mixin"))throw new AssertionError("Opt-out");checks++;}finally{if(previous==null)System.clearProperty("prominence.weaponAbilityHud");else System.setProperty("prominence.weaponAbilityHud",previous);}
        System.out.println("PASS: "+checks+" weapon HUD actual native version/fingerprint/modified/missing/opt-out checks; no game");
    }
}
