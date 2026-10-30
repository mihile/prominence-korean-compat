package local.prominence.rewards;
import com.google.gson.*;import java.io.*;import java.util.zip.*;import java.nio.charset.StandardCharsets;import java.util.*;import net.fabricmc.fabric.api.networking.v1.*;import net.minecraft.*;
public final class RewardWire {
 public static final class_2960 CHANNEL=new class_2960("prominence_korean_compat","reward_scaling_v2");
 public static final class_2960 DISPLAY_CHANNEL=new class_2960("prominence_korean_compat","reward_scaling_v3");
 public static final int MAX_BYTES=900000,MAX_PLANS=16384,MAX_JSON_BYTES=16777216;private static final Gson GSON=new Gson();
 public record Packet(int schema,List<RewardDefinition> plans){}
 private static List<RewardDefinition> packedPlans;private static byte[] packedBytes,legacyBytes;
 private RewardWire(){}
 public static byte[] encode(List<RewardDefinition> plans){
  return encode(plans,3);
 }
 private static byte[] encode(List<RewardDefinition> plans,int schema){
  if(plans.size()>MAX_PLANS||plans.stream().anyMatch(p->!p.valid()))throw new IllegalArgumentException("Reward metadata bounds");
  byte[] json=GSON.toJson(new Packet(schema,plans)).getBytes(StandardCharsets.UTF_8);if(json.length>MAX_JSON_BYTES)throw new IllegalArgumentException("Reward expanded size");
  try{var out=new ByteArrayOutputStream();try(var gzip=new GZIPOutputStream(out)){gzip.write(json);}var bytes=out.toByteArray();if(bytes.length>MAX_BYTES)throw new IllegalArgumentException("Reward packet size");return bytes;}catch(IOException e){throw new IllegalArgumentException("Reward compression",e);}
 }
 public static List<RewardDefinition> decode(byte[] bytes){
  if(bytes.length>MAX_BYTES)throw new IllegalArgumentException("Reward packet size");byte[] json;
  try(var in=new GZIPInputStream(new ByteArrayInputStream(bytes));var out=new ByteArrayOutputStream()){
   byte[] buffer=new byte[8192];int n,total=0;while((n=in.read(buffer))!=-1){total+=n;if(total>MAX_JSON_BYTES)throw new IllegalArgumentException("Reward expanded size");out.write(buffer,0,n);}json=out.toByteArray();
  }catch(IOException e){throw new IllegalArgumentException("Reward compression",e);}
  int depth=0;boolean string=false,escape=false;for(byte b:json){if(string){if(escape)escape=false;else if(b=='\\')escape=true;else if(b=='"')string=false;}else if(b=='"')string=true;else if(b=='{'||b=='['){if(++depth>16)throw new IllegalArgumentException("Reward metadata nesting");}else if(b=='}'||b==']')depth--;}
  var packet=GSON.fromJson(new String(json,StandardCharsets.UTF_8),Packet.class);if(packet==null||!Set.of(2,3).contains(packet.schema())||packet.plans()==null||packet.plans().size()>MAX_PLANS||packet.plans().stream().anyMatch(p->p==null||!p.valid()||packet.schema()==2&&p.bonusRolls()!=0))throw new IllegalArgumentException("Reward metadata bounds");return List.copyOf(packet.plans());
 }
 public static synchronized byte[] payload(List<RewardDefinition> plans){
  if(plans!=packedPlans){var bytes=encode(plans);packedPlans=plans;packedBytes=bytes;legacyBytes=null;}return packedBytes;
 }
 public static synchronized byte[] legacyPayload(List<RewardDefinition> plans){
  payload(plans);
  // An older client must never silently ignore a native bonus-roll coefficient.
  if(legacyBytes==null)legacyBytes=encode(plans.stream().filter(p->p.bonusRolls()==0).toList(),2);
  return legacyBytes;
 }
 public static void send(class_3222 player){
  try {
  var plans=RewardRegistry.snapshot(player.field_13995.method_3772());
  boolean modern=ServerPlayNetworking.canSend(player,DISPLAY_CHANNEL);
  if(!modern&&!ServerPlayNetworking.canSend(player,CHANNEL))return;
  var bytes=modern?payload(plans):legacyPayload(plans);var buf=PacketByteBufs.create();buf.writeBytes(bytes);ServerPlayNetworking.send(player,modern?DISPLAY_CHANNEL:CHANNEL,buf);
  org.slf4j.LoggerFactory.getLogger("Prominence compatibility").info("Reward metadata sent: {} pools / {} compressed bytes",plans.size(),bytes.length);
  } catch(RuntimeException e) {
   // Optional display metadata must never reject login or disable gameplay bonuses.
   // Do not retry the same canSend/send operation from the failure handler.
   org.slf4j.LoggerFactory.getLogger("Prominence compatibility").warn("Rare reward metadata sync skipped; player login and gameplay are unchanged",e);
  }
 }
}
