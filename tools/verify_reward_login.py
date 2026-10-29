"""Execute production login callbacks/wire with Fabric constructor-phase API doubles."""
from pathlib import Path
import argparse,json,os,subprocess,zipfile
from build import safe_arg
p=argparse.ArgumentParser();p.add_argument('--output',type=Path,required=True);p.add_argument('--classpath',type=Path,required=True);p.add_argument('--java-home',type=Path,required=True);p.add_argument('--baseline',type=Path,required=True);a=p.parse_args();r=Path(__file__).resolve().parents[1];w=a.output.resolve();w.mkdir(parents=True,exist_ok=True)
stubs={
'net/fabricmc/api/ModInitializer':'public interface ModInitializer {void onInitialize();}',
'fixture/Event':'public class Event<T> {public final java.util.List<T> callbacks=new java.util.ArrayList<>();public void register(T cb){callbacks.add(cb);}}',
'net/fabricmc/fabric/api/networking/v1/S2CPlayChannelEvents':'public class S2CPlayChannelEvents {public interface Register {void onChannelRegister(net.minecraft.class_3244 h,Object sender,net.minecraft.server.MinecraftServer s,java.util.List<net.minecraft.class_2960> channels);}public static final fixture.Event<Register> REGISTER=new fixture.Event<>();}',
'net/fabricmc/fabric/api/networking/v1/ServerPlayConnectionEvents':'public class ServerPlayConnectionEvents {public interface Join {void onPlayReady(net.minecraft.class_3244 h,Object sender,net.minecraft.server.MinecraftServer s);}public interface Disconnect {void onPlayDisconnect(net.minecraft.class_3244 h,net.minecraft.server.MinecraftServer s);}public static final fixture.Event<Join> JOIN=new fixture.Event<>();public static final fixture.Event<Disconnect> DISCONNECT=new fixture.Event<>();}',
'net/fabricmc/fabric/api/event/lifecycle/v1/ServerTickEvents':'public class ServerTickEvents {public interface End {void onEndTick(net.minecraft.server.MinecraftServer s);}public static final fixture.Event<End> END_SERVER_TICK=new fixture.Event<>();}',
'net/fabricmc/fabric/api/networking/v1/PacketByteBufs':'public class PacketByteBufs {public static class Buffer {public void writeBytes(byte[] b){}}public static Buffer create(){return new Buffer();}}',
'net/fabricmc/fabric/api/networking/v1/ServerPlayNetworking':'public class ServerPlayNetworking {public static int queries,sends;public static boolean failQuery,failSend,supported=true;public static boolean canSend(net.minecraft.class_3222 p,net.minecraft.class_2960 c){queries++;if(p.field_13987==null||!p.field_13987.ready)throw new NullPointerException("network addon not published");if(failQuery)throw new IllegalStateException("query failure");return supported;}public static void send(net.minecraft.class_3222 p,net.minecraft.class_2960 c,PacketByteBufs.Buffer b){if(failSend)throw new IllegalStateException("send failure");sends++;}}',
'net/minecraft/class_2960':'public class class_2960 {public class_2960(String a,String b){}}',
'net/minecraft/class_3222':'public class class_3222 {public net.minecraft.server.MinecraftServer field_13995;public class_3244 field_13987;public class_3222(net.minecraft.server.MinecraftServer s){field_13995=s;}}',
'net/minecraft/class_3244':'public class class_3244 {public class_3222 field_14140;public boolean ready;public class_3244(class_3222 p){field_14140=p;p.field_13987=this;}}',
'net/minecraft/server/MinecraftServer':'public class MinecraftServer {public void execute(Runnable task){task.run();}public Object method_3772(){return this;}}',
'local/prominence/rewards/RewardRegistry':'public class RewardRegistry {public static int snapshots,disabled;public static boolean fail;public static java.util.List<RewardDefinition> snapshot(Object recipes){snapshots++;if(fail)throw new IllegalArgumentException("recipe failure");return java.util.List.of();}public static void disable(){disabled++;}}',
'org/slf4j/LoggerFactory':'public class LoggerFactory {public static Log getLogger(String n){return new Log();}public static class Log {public void warn(String s,Throwable t){}public void info(String s,Object... args){}}}'
}
fixture="""package fixture;
import local.prominence.rewards.*;import net.fabricmc.fabric.api.networking.v1.*;import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;import net.minecraft.*;import net.minecraft.server.MinecraftServer;
public class LoginTests {
 static int checks;static void check(boolean v,String why){checks++;if(!v)throw new AssertionError(why);}
 static void register(class_3244 h){for(var cb:S2CPlayChannelEvents.REGISTER.callbacks)cb.onChannelRegister(h,null,h.field_14140.field_13995,java.util.List.of(RewardWire.CHANNEL));}
 static void join(class_3244 h){for(var cb:ServerPlayConnectionEvents.JOIN.callbacks)cb.onPlayReady(h,null,h.field_14140.field_13995);}
 static void tick(MinecraftServer s){for(var cb:ServerTickEvents.END_SERVER_TICK.callbacks)cb.onEndTick(s);}
 static void disconnect(class_3244 h){for(var cb:ServerPlayConnectionEvents.DISCONNECT.callbacks)cb.onPlayDisconnect(h,h.field_14140.field_13995);}
 public static void main(String[] args){
  new RewardCommonInit().onInitialize();var server=new MinecraftServer();var h=new class_3244(new class_3222(server));
  register(h);check(ServerPlayNetworking.queries==0&&RewardRegistry.snapshots==0,"constructor REGISTER must not query unpublished addon");
  h.ready=true;join(h);register(h);tick(server);check(ServerPlayNetworking.sends==1,"constructor+JOIN duplicate coalesced at safe tick");
  register(h);check(ServerPlayNetworking.sends==1,"late registration not inline");tick(server);check(ServerPlayNetworking.sends==2,"late registration delivered after tick");
  register(h);disconnect(h);tick(server);check(ServerPlayNetworking.sends==2,"disconnected pending delivery removed");
  register(h);var replacement=new class_3244(h.field_14140);replacement.ready=true;tick(server);check(ServerPlayNetworking.sends==2,"old connection identity ignored");
  var second=new MinecraftServer();var other=new class_3244(new class_3222(second));other.ready=true;join(replacement);join(other);tick(second);check(ServerPlayNetworking.sends==3,"only selected server drained");tick(server);check(ServerPlayNetworking.sends==4,"other server pending delivery retained");
  ServerPlayNetworking.failQuery=true;int q=ServerPlayNetworking.queries;register(replacement);tick(server);check(ServerPlayNetworking.queries==q+1,"failed canSend never retried from catch");check(RewardRegistry.disabled==0,"network failure does not disable gameplay scaling");tick(server);check(ServerPlayNetworking.queries==q+1,"no retry storm");ServerPlayNetworking.failQuery=false;
  ServerPlayNetworking.failSend=true;register(replacement);tick(server);check(ServerPlayNetworking.sends==4&&RewardRegistry.disabled==0,"send failure cannot escape tick or disable bonuses");ServerPlayNetworking.failSend=false;
  RewardRegistry.fail=true;register(replacement);tick(server);check(RewardRegistry.disabled==0,"metadata recipe failure cannot reject login");RewardRegistry.fail=false;
  ServerPlayNetworking.supported=false;join(replacement);tick(server);check(ServerPlayNetworking.sends==4,"older client without metadata channel still joins");ServerPlayNetworking.supported=true;
  // Also call production Wire directly in the dangerous phase: even defensive fallback must not throw.
  replacement.ready=false;q=ServerPlayNetworking.queries;RewardWire.send(replacement.field_14140);check(ServerPlayNetworking.queries==q+1,"direct early call swallowed once");replacement.ready=true;
  join(replacement);tick(server);check(ServerPlayNetworking.sends==5,"subsequent normal metadata delivery recovers");
  System.out.println("PASS "+checks+" production login/wire constructor timing, inline executor, JOIN/late registration, coalescing, disconnect, replacement, multi-server and nonfatal failure checks; API doubles, no game");
 }
}
"""
cp=os.pathsep.join(map(safe_arg,json.loads(a.classpath.read_text('utf8'))));results={}
for mode in ['baseline','fixed']:
 src=w/mode/'src';classes=w/mode/'classes';classes.mkdir(parents=True,exist_ok=True)
 for n,body in stubs.items():
  f=src/(n+'.java');f.parent.mkdir(parents=True,exist_ok=True);f.write_text('package '+n.rsplit('/',1)[0].replace('/','.')+';'+body,'utf8')
 f=src/'fixture/LoginTests.java';f.parent.mkdir(parents=True,exist_ok=True);f.write_text(fixture,'utf8')
 for name in ['RewardCommonInit.java','RewardWire.java','RewardDefinition.java']:
  f=src/'local/prominence/rewards'/name;f.parent.mkdir(parents=True,exist_ok=True)
  if mode=='baseline':
   with zipfile.ZipFile(a.baseline) as z:raw=z.read('META-INF/prominence-korean-compat-source/auto-update/local/prominence/rewards/'+name)
   f.write_bytes(raw)
  else:f.write_bytes((r/'src/main/java/local/prominence/rewards'/name).read_bytes())
 opts=['--release','17','-proc:none','-encoding','UTF-8','-cp',cp,'-d',safe_arg(classes),*[safe_arg(f) for f in src.rglob('*.java')]];af=w/(mode+'-compile.args');af.write_text('\n'.join('"'+x+'"' for x in opts),'utf8')
 q=subprocess.run([str(a.java_home/'bin/javac.exe'),'@'+safe_arg(af)],capture_output=True,text=True,encoding='utf8',errors='replace');(w/(mode+'-compile.log')).write_text(q.stdout+q.stderr,'utf8');assert q.returncode==0,q.stderr
 opts=['-cp',safe_arg(classes)+os.pathsep+cp,'fixture.LoginTests'];af=w/(mode+'-run.args');af.write_text('\n'.join('"'+x+'"' for x in opts),'utf8')
 q=subprocess.run([str(a.java_home/'bin/java.exe'),'@'+safe_arg(af)],cwd=w,capture_output=True,text=True,encoding='utf8',errors='replace');(w/(mode+'-run.log')).write_text(q.stdout+q.stderr,'utf8')
 if mode=='baseline':assert q.returncode!=0 and 'network addon not published' in q.stderr;print('REPRODUCED original invalid-player-data cause: inline REGISTER queried unpublished addon')
 else:assert q.returncode==0,q.stdout+q.stderr;print(q.stdout.strip())
 results[mode]='reproduced' if mode=='baseline' else 'passed'
(w/'report.json').write_text(json.dumps({'status':'passed','results':results,'productionInitializerAndWireExecuted':True,'gameStarted':False,'playerDataTouched':False},indent=2)+'\n','utf8')
