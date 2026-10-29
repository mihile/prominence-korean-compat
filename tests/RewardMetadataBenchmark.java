package local.prominence.rewards;
import com.google.gson.*;import java.nio.file.*;import java.util.*;import local.prominence.rewards.client.RewardClientState;
/** Offline timings, not gameplay frame timings. Larger input repeats real rows as a stress case. */
public final class RewardMetadataBenchmark {
 interface Work {void run();}
 static Map<String,Double> time(Work action){var times=new double[15];for(int n=0;n<5;n++)action.run();for(int n=0;n<times.length;n++){long t=System.nanoTime();action.run();times[n]=(System.nanoTime()-t)/1e6;}Arrays.sort(times);return Map.of("medianMs",times[7],"maxMs",times[14]);}
 public static void main(String[] args)throws Exception{
  var original=List.of(new Gson().fromJson(Files.readString(Path.of(args[0])),RewardDefinition[].class));var report=new LinkedHashMap<String,Object>();report.put("gameStarted",false);report.put("largerInputIsRepeatedNativeRowsNotNewLiveSnapshot",true);
  for(int count:new int[]{original.size(),5510}){
   if(count>RewardWire.MAX_PLANS)continue;
   var input=new ArrayList<RewardDefinition>();for(int n=0;n<count;n++)input.add(original.get(n%original.size()));var rows=List.copyOf(input);long start=System.nanoTime();byte[] packet=RewardWire.encode(rows);double coldEncode=(System.nanoTime()-start)/1e6;start=System.nanoTime();var decoded=RewardWire.decode(packet);double coldDecode=(System.nanoTime()-start)/1e6;
   var data=new LinkedHashMap<String,Object>();data.put("rows",count);data.put("compressedBytes",packet.length);data.put("coldEncodeMs",coldEncode);data.put("coldDecodeMs",coldDecode);data.put("encode",time(()->RewardWire.encode(rows)));data.put("decode",time(()->RewardWire.decode(packet)));data.put("install",time(()->RewardClientState.install(decoded)));report.put("rows"+count,data);
  }
  RewardClientState.clear();String json=new GsonBuilder().setPrettyPrinting().create().toJson(report);Files.writeString(Path.of(args[1]),json);System.out.println(json);
 }
}
