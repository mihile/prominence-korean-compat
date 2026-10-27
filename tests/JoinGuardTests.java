package local.prominence.join;

import com.google.gson.JsonParser;
import java.nio.file.*;
import java.util.*;

public final class JoinGuardTests {
    public static void main(String[] args)throws Exception {
        var pins=JsonParser.parseString(Files.readString(Path.of(args[0]))).getAsJsonObject();int count=0;
        for(var entry:pins.entrySet()) {
            var group=entry.getValue().getAsJsonObject();var versions=new HashMap<String,String>();var bytes=new HashMap<String,byte[]>();versions.put("minecraft","1.20.1");versions.put("mixinextras","0.5.4");
            for(var mod:group.entrySet()) {versions.put(mod.getKey(),mod.getValue().getAsJsonObject().get("version").getAsString());for(var cl:mod.getValue().getAsJsonObject().getAsJsonObject("classes").entrySet())try(var in=JoinGuardTests.class.getClassLoader().getResourceAsStream(cl.getKey())){bytes.put(cl.getKey(),in.readAllBytes());}}
            if(!JoinGuard.matches(group,versions::get,(id,path)->bytes.get(path)))throw new AssertionError("Installed pins failed "+entry.getKey());count++;
            if(JoinGuard.matches(group,versions::get,(id,path)->null))throw new AssertionError("Missing class allowed");count++;
            if(JoinGuard.matches(group,versions::get,(id,path)->new byte[]{1}))throw new AssertionError("Changed class allowed");count++;
            versions.put("minecraft","1.21");if(JoinGuard.matches(group,versions::get,(id,path)->bytes.get(path)))throw new AssertionError("MC drift allowed");count++;
            versions.put("minecraft","1.20.1");versions.put("mixinextras","0.5.5");if(JoinGuard.matches(group,versions::get,(id,path)->bytes.get(path)))throw new AssertionError("MixinExtras drift allowed");count++;
        }
        System.out.println("PASS: join native guard scenarios="+count);
    }
}
