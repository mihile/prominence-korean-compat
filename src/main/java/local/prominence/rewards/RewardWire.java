package local.prominence.rewards;
import com.google.gson.*;import java.nio.charset.StandardCharsets;import java.util.*;import net.fabricmc.fabric.api.networking.v1.*;import net.minecraft.*;
public final class RewardWire {
 public static final class_2960 CHANNEL=new class_2960("prominence_korean_compat","reward_scaling_v1");
 public static final int MAX_BYTES=900000,MAX_PLANS=4096;private static final Gson GSON=new Gson();
 public record Packet(int schema,List<RewardDefinition> plans){}
 private RewardWire(){}
 public static byte[] encode(List<RewardDefinition> plans){if(plans.size()>MAX_PLANS||plans.stream().anyMatch(p->!p.valid()))throw new IllegalArgumentException("Reward metadata bounds");var bytes=GSON.toJson(new Packet(1,plans)).getBytes(StandardCharsets.UTF_8);if(bytes.length>MAX_BYTES)throw new IllegalArgumentException("Reward metadata size");return bytes;}
 public static List<RewardDefinition> decode(byte[] bytes){if(bytes.length>MAX_BYTES)throw new IllegalArgumentException("Reward metadata size");int depth=0;boolean string=false,escape=false;for(byte b:bytes){if(string){if(escape)escape=false;else if(b=='\\')escape=true;else if(b=='"')string=false;}else if(b=='"')string=true;else if(b=='{'||b=='['){if(++depth>16)throw new IllegalArgumentException("Reward metadata nesting");}else if(b=='}'||b==']')depth--;}
  var packet=GSON.fromJson(new String(bytes,StandardCharsets.UTF_8),Packet.class);if(packet==null||packet.schema()!=1||packet.plans()==null||packet.plans().size()>MAX_PLANS||packet.plans().stream().anyMatch(p->p==null||!p.valid()))throw new IllegalArgumentException("Reward metadata bounds");return List.copyOf(packet.plans());}
 public static void send(class_3222 player){
  try {
  var plans=RewardRegistry.snapshot(player.field_13995.method_3772());
  if(!ServerPlayNetworking.canSend(player,CHANNEL))return;
  var buf=PacketByteBufs.create();buf.writeBytes(encode(plans));ServerPlayNetworking.send(player,CHANNEL,buf);
  } catch(RuntimeException e) {
   // Optional display metadata must never reject login or disable gameplay bonuses.
   // Do not retry the same canSend/send operation from the failure handler.
   org.slf4j.LoggerFactory.getLogger("Prominence compatibility").warn("Rare reward metadata sync skipped; player login and gameplay are unchanged",e);
  }
 }
}
