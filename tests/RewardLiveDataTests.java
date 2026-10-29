package local.prominence.rewards;
import com.google.gson.*;import java.nio.file.*;import java.util.*;import java.io.*;import java.util.zip.*;import local.prominence.rewards.client.*;import net.minecraft.*;
/** Captured native server data, never a fabricated replacement for the server loot tables. */
public class RewardLiveDataTests {
 static int checks;static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
 public static void main(String[] args)throws Exception{
  String json=Files.readString(Path.of(args[0]));var rows=List.of(new Gson().fromJson(json,RewardDefinition[].class));check(rows.size()>1000,"real complete server dataset");check(rows.stream().allMatch(RewardDefinition::valid),"all live definitions valid");
  byte[] packet=RewardWire.encode(rows);check(packet.length<RewardWire.MAX_BYTES,"actual full dataset fits packet");check(RewardWire.decode(packet).equals(rows),"compressed full dataset lossless");
  var cached=RewardWire.payload(rows);check(RewardWire.payload(rows)==cached,"same reload reuses encoded metadata");var fresh=new ArrayList<>(rows);check(RewardWire.payload(fresh)!=cached,"new reload identity invalidates encoded metadata");
  RewardClientState.install(rows);
  var gold=RewardClientState.find("minecraft:entities/piglin_brute",List.of("adventurez:gilded_blackstone_shard"),10,List.of());check(gold!=null,"actual gold shard live route matched");
  var a=new RewardClientText.Attachment(gold,gold.outputs().get(0));check(RewardClientText.lines(a,3,0,true,false).get(0).equals("드롭 확률: 22% (기본 10%)"),"gold Looting III actually displayed");check(RewardClientText.lines(a,3,10,true,false).get(0).equals("드롭 확률: 27% (기본 10%)"),"gold Luck actually displayed");
  var lich=rows.stream().filter(d->d.table().equals("bosses_of_mass_destruction:entities/lich")&&d.outputs().stream().anyMatch(o->o.item().equals("simplyswords:slumbering_lichblade"))).findFirst().orElseThrow();
  var ids=lich.outputs().stream().map(RewardDefinition.Output::item).toList();var chance=class_2561.method_43469("emi_loot.condition.chance",class_2561.method_43470("30.0"));
  var match=RewardClientState.find(lich.table(),ids,100./38,List.of(chance,class_2561.method_43471("emi_loot.function.set_nbt")));check(match==lich,"actual grouped SimplySwords slot including NBT function matched");
  var item=lich.outputs().stream().filter(o->o.item().equals("simplyswords:slumbering_lichblade")).findFirst().orElseThrow();var lines=RewardClientText.lines(new RewardClientText.Attachment(lich,item),3,10,true,true);check(lines.get(0).equals("드롭 확률: 0.78%"),"actual individual unique probability");check(lines.get(1).contains("30%")&&lines.get(1).contains("2.63%"),"30% category and one-of38 explained");
  check(lich.lootingRate()==0&&lich.luckRate()==0,"display-only 30% pool not silently buffed");
  byte[] broken=packet.clone();broken[broken.length-2]^=1;try{RewardWire.decode(broken);throw new AssertionError("corrupt compressed packet accepted");}catch(IllegalArgumentException expected){checks++;}
  var out=new ByteArrayOutputStream();try(var gz=new GZIPOutputStream(out)){byte[] block=new byte[8192];for(int n=0;n<=RewardWire.MAX_JSON_BYTES;n+=block.length)gz.write(block);}try{RewardWire.decode(out.toByteArray());throw new AssertionError("expanded bound ignored");}catch(IllegalArgumentException expected){checks++;}
  RewardClientState.clear();check(RewardClientState.find(gold.table(),List.of("adventurez:gilded_blackstone_shard"),10,List.of())==null,"old-server/session fallback remains safe");
  System.out.println("PASS "+checks+" actual server dataset rows="+rows.size()+" JSON="+json.length()+" compressed="+packet.length+"; gold22/27%, SimplySwords0.78%, compression corruption/bounds and session checks; no game");
 }
}
