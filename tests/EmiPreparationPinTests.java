import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import local.prominence.join.JoinGuard;

public final class EmiPreparationPinTests {
    public static void main(String[] args)throws Exception {
        var groups=JsonParser.parseString(Files.readString(Path.of(args[0]))).getAsJsonObject();int checks=0;
        for(var entry:groups.entrySet()) {
            var group=entry.getValue().getAsJsonObject();var versions=new HashMap<String,String>();versions.put("minecraft","1.20.1");versions.put("mixinextras","0.5.4");
            for(var mod:group.entrySet())versions.put(mod.getKey(),mod.getValue().getAsJsonObject().get("version").getAsString());
            boolean result=JoinGuard.matches(group,versions::get,(id,path)->{try(var in=EmiPreparationPinTests.class.getClassLoader().getResourceAsStream(path)){return in==null?null:in.readAllBytes();}catch(Exception e){return null;}});
            if(!result)throw new AssertionError("Current actual resources fail "+entry.getKey());checks++;
        }
        System.out.println("PASS: actual installed EMI preparation resource fingerprints="+checks);
    }
}
