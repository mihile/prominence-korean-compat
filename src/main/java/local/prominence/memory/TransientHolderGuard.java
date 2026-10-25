package local.prominence.memory;

import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import net.fabricmc.loader.api.FabricLoader;

final class TransientHolderGuard {
    private TransientHolderGuard() {}
    static Map<String,String> pinned(String id) {
        return switch(id) {
            case "porting_lib_entity" -> Map.of(
                "io/github/fabricators_of_create/porting_lib/entity/mixin/client/MultiPlayerGameModeMixin.class","72ed606263ce10e7f3eb4a9529d39dd17f3e3467ca825172263c7295652f0d26",
                "io/github/fabricators_of_create/porting_lib/entity/events/PlayerEvents.class","444849afbb0bd8f3c09fbb9aea950d53689d0a174a2c0fbe918c6d0adfba9892",
                "io/github/fabricators_of_create/porting_lib/entity/events/PlayerInteractionEvents$LeftClickBlock.class","8254a56212caa5e8dc3400c1cce43913444580cffcf2115a783ee42efa66c4fc");
            case "immersive_melodies" -> Map.of("immersive_melodies/client/MelodyProgressManager.class","c036702508a3352f387415d4c5f6a5e8ba59d011944a0ff0637cffd91b409f96");
            default -> Map.of();
        };
    }
    static boolean matches(String id,String version,Map<String,byte[]> bytes) {
        String expected=switch(id){case "porting_lib_entity"->"2.3.13+1.20.1";case "immersive_melodies"->"0.5.0+1.20.1";default->"";};
        if(expected.isEmpty()||!expected.equals(version))return false;
        try {
            for(var entry:pinned(id).entrySet()) {
                byte[] b=bytes.get(entry.getKey());
                if(b==null||!HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(b)).equals(entry.getValue()))return false;
            }
            return true;
        }catch(Exception e){return false;}
    }
    static boolean installed(String id) {
        if(System.getProperty("prominence.transientWorldHolders","true").equalsIgnoreCase("false"))return false;
        try {
            var loader=FabricLoader.getInstance();var mod=loader.getModContainer(id);
            if(!loader.getModContainer("minecraft").map(m->m.getMetadata().getVersion().getFriendlyString().equals("1.20.1")).orElse(false)||mod.isEmpty())return false;
            Map<String,byte[]> bytes=new java.util.HashMap<>();
            for(String name:pinned(id).keySet()) {
                var path=mod.get().findPath(name);if(path.isPresent())bytes.put(name,Files.readAllBytes(path.get()));
            }
            return matches(id,mod.get().getMetadata().getVersion().getFriendlyString(),bytes);
        }catch(Exception|LinkageError e){return false;}
    }
}
