package local.prominence.rewards;
import com.google.gson.Gson;import java.nio.file.*;import java.util.*;
/** Run in separate JVMs with the real previous/current JAR, using the shared ABI only. */
public final class RewardProtocolFixture {
 public static void main(String[] args)throws Exception{
  Path dir=Path.of(args[1]);
  if(args[0].equals("emit-old")){
   var d=new RewardDefinition("test:entities/mob","binomial",.1,0,0,true,List.of(new RewardDefinition.Output("test:drop",1,10,0,true)),List.of(),List.of(),List.of());
   Files.write(dir.resolve("old-v2.gz"),RewardWire.encode(List.of(d)));
  }else if(args[0].equals("emit-new")){
   var normal=RewardWire.decode(Files.readAllBytes(dir.resolve("old-v2.gz"))).get(0);
   var bonus=new Gson().fromJson("{\"table\":\"test:entities/boss\",\"kind\":\"weighted\",\"base\":1,\"lootingRate\":0,\"luckRate\":0,\"combat\":true,\"outputs\":[{\"item\":\"test:ring\",\"share\":0.5,\"expected\":100,\"index\":0,\"unique\":true}],\"weights\":[1,1],\"boosts\":[false,false],\"draws\":[0,0,1],\"bonusRolls\":0.2}",RewardDefinition.class);
   if(!bonus.valid())throw new AssertionError("bonus plan");
   var plans=List.of(normal,bonus);Files.write(dir.resolve("new-v3.gz"),RewardWire.encode(plans));
   byte[] legacy=(byte[])RewardWire.class.getMethod("legacyPayload",List.class).invoke(null,plans);Files.write(dir.resolve("safe-v2.gz"),legacy);
   if(RewardWire.decode(Files.readAllBytes(dir.resolve("old-v2.gz"))).size()!=1||RewardWire.decode(Files.readAllBytes(dir.resolve("new-v3.gz"))).size()!=2)throw new AssertionError("new reader");
  }else if(args[0].equals("read-old")){
   if(RewardWire.decode(Files.readAllBytes(dir.resolve("safe-v2.gz"))).size()!=1)throw new AssertionError("old reader did not receive safe subset");
   try{RewardWire.decode(Files.readAllBytes(dir.resolve("new-v3.gz")));throw new AssertionError("old reader accepted unknown schema");}catch(IllegalArgumentException expected){}
  }else throw new IllegalArgumentException(args[0]);
  System.out.println("PASS protocol "+args[0]);
 }
}
