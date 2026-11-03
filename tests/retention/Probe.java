package retentionfixture;
import java.lang.ref.*;
import java.lang.reflect.*;
import java.util.*;
import net.minecraft.*;
import local.prominence.memory.*;
import lol.bai.badpackets.impl.registry.ChannelRegistry;
import lol.bai.badpackets.impl.handler.*;
import de.mari_023.ae2wtlib.wct.CraftingTerminalHandler;
import com.anthonyhilyard.legendarytooltips.config.LegendaryTooltipsConfig;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;
import mod.azure.azurelibarmor.model.GeoModel;
import mod.azure.azurelibarmor.core.molang.*;
import dev.shadowsoffire.apotheosis.util.Events.RepairEvent;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.spell_engine.api.spell.SpellContainer;

public final class Probe {
 static int checks;
 static void check(boolean b,String message){checks++;if(!b)throw new AssertionError(message);}
 static final class Slot implements RetainedClientOwner {
  Object value;Slot(Object value){this.value=value;}
  public Object prominence$retainedOwner(){return value;}
  public void prominence$releaseOwner(){value=null;}
 }
 static void tick(RetentionCleanupClient cleanup,class_310 client)throws Exception {
  Method m=cleanup.getClass().getDeclaredMethod("afterTick",class_310.class);m.setAccessible(true);m.invoke(cleanup,client);
 }
 static WeakReference<class_638> cycle() {
  var w=new class_638();var p=new class_746(w);new CraftingTerminalHandler(p);return new WeakReference<>(w);
 }
 @SuppressWarnings("unchecked") public static void main(String[]args)throws Exception {
  var cleanup=new RetentionCleanupClient();Field f=cleanup.getClass().getDeclaredField("enabled");f.setAccessible(true);Map<String,Boolean> flags=(Map<String,Boolean>)f.get(cleanup);
  for(String key:List.of("BadPackets","Ae2","Legendary","Zenith","Azure","Screens","Armor","StackKeys","ArmorRenderers"))flags.put(key,true);
  var old=new class_638();var now=new class_638();var server=new class_1937();var player=new class_746(now);var previous=new class_746(old);
  var client=new class_310();client.field_1687=now;client.field_1724=player;client.connection=new class_634();
  var pendingNet=new class_2535(true);var active=new ClientPacketHandler(client.connection,new class_2535(true));var stale=new ClientPacketHandler(new class_634(),new class_2535(false));var pending=new ClientPacketHandler(new class_634(),pendingNet);
  new CraftingTerminalHandler(previous);var remotePrevious=new class_1657(old);new CraftingTerminalHandler(remotePrevious);var currentTerminal=new CraftingTerminalHandler(player);var serverPlayer=new class_1657(server);new CraftingTerminalHandler(serverPlayer);
  LegendaryTooltipsConfig.cache().put(new Object(),new Object());ItemStackKey.cache().put(new Object(),new Object());
  var event=new RepairEvent(previous);class_1706.zenith$event=event;class_1706.zenith$leftItem=new class_1799();
  Slot oldRender=new Slot(new class_1297(old)),liveRender=new Slot(new class_1297(now)),serverRender=new Slot(new class_1297(server));
  RetentionCleanupClient.trackAzure(oldRender);RetentionCleanupClient.trackAzure(liveRender);RetentionCleanupClient.trackAzure(serverRender);
  Slot oldArmor=new Slot(new class_1297(old)),liveArmor=new Slot(new class_1297(now));RetentionCleanupClient.trackArmor(oldArmor);RetentionCleanupClient.trackArmor(liveArmor);
  Slot oldBack=new Slot(new class_490(new class_1723(previous))),liveBack=new Slot(new class_490(new class_1723(player))),menuBack=new Slot(new class_437());
  RetentionCleanupClient.trackScreen(oldBack);RetentionCleanupClient.trackScreen(liveBack);RetentionCleanupClient.trackScreen(menuBack);
  GeoModel.query("old",old);GeoModel.query("current",now);
  MolangParser.setMemoizedValue("foreign",()->old.hashCode());LazyVariable oldVar=MolangParser.VARIABLES.get("old"),currentVar=MolangParser.VARIABLES.get("current"),foreign=MolangParser.VARIABLES.get("foreign");double value=currentVar.get();
  tick(cleanup,client);
  check(!ChannelRegistry.S2C.handlers.contains(stale),"closed packet handler retained");check(ChannelRegistry.S2C.handlers.contains(active)&&ChannelRegistry.S2C.handlers.contains(pending),"active/login packet handler removed");
  check(!CraftingTerminalHandler.players().containsKey(previous),"weak-key/value cycle retained");check(CraftingTerminalHandler.players().get(player)==currentTerminal&&CraftingTerminalHandler.players().containsKey(serverPlayer),"current/server terminal removed");
  check(!CraftingTerminalHandler.players().containsKey(remotePrevious),"remote client player retained old world");
  check(class_1706.zenith$event==null&&class_1706.zenith$leftItem==class_1799.field_8037,"old anvil scratch retained");
  check(oldRender.value==null&&liveRender.value!=null&&serverRender.value!=null,"render ownership crossed world boundaries");
  check(oldArmor.value==null&&liveArmor.value!=null,"armor renderer ownership crossed world boundaries");
  check(oldBack.value==null&&liveBack.value!=null&&menuBack.value!=null,"previous-screen navigation not preserved");
  check(oldVar==MolangParser.VARIABLES.get("old")&&oldVar.get()==0,"Molang variable identity not retained");check(currentVar.get()==value&&foreign.get()!=0,"current/foreign Molang changed");
  check(LegendaryTooltipsConfig.cache().isEmpty()&&LegendaryTooltipsConfig.colors.size()==1,"tooltip native config changed");check(ItemStackKey.cache().isEmpty(),"client key cache not cleared");
  class_1706.zenith$event=new RepairEvent(player);tick(cleanup,client);check(class_1706.zenith$event.player==player,"current repair event cleared");
  // Unbounded tooltip memoization is limited without a world transition.
  for(int i=0;i<4100;i++)LegendaryTooltipsConfig.cache().put(new Object(),i);
  for(int i=0;i<20;i++)tick(cleanup,client);
  check(LegendaryTooltipsConfig.cache().isEmpty(),"tooltip cap not enforced");
  pendingNet.open=false;for(int i=0;i<20;i++)tick(cleanup,client);check(!ChannelRegistry.S2C.handlers.contains(pending),"delayed socket close retained handler");
  // Integrated-server shared scratch/maps must not be touched by a client tick.
  client.server=new class_1132();ItemStackKey.cache().put("server-work",new Object());class_1706.zenith$event=new RepairEvent(previous);
  client.field_1687=new class_638();client.field_1724=new class_746(client.field_1687);new CraftingTerminalHandler(previous);tick(cleanup,client);
  check(ItemStackKey.cache().containsKey("server-work")&&class_1706.zenith$event.player==previous&&CraftingTerminalHandler.players().containsKey(previous),"integrated-server scratch changed");
  check(flags.values().stream().allMatch(Boolean::booleanValue),"a module silently disabled in fixture");
  client.server=null;WeakReference<class_638> weak=cycle();System.gc();check(weak.get()!=null,"weak-key cycle not reproduced");
  Method ae=cleanup.getClass().getDeclaredMethod("ae2",class_638.class,class_746.class);ae.setAccessible(true);ae.invoke(cleanup,client.field_1687,client.field_1724);
  for(int i=0;i<30&&weak.get()!=null;i++){System.gc();Thread.sleep(10);}check(weak.get()==null,"detached terminal still retains world");
  // A replacement written reentrantly by a predicate must survive cleanup.
  Method release=cleanup.getClass().getDeclaredMethod("releaseOwners",Map.class,java.util.function.Predicate.class);release.setAccessible(true);
  Slot replaced=new Slot(old);var registry=new WeakHashMap<RetainedClientOwner,Boolean>();registry.put(replaced,true);
  int n=(int)release.invoke(null,registry,(java.util.function.Predicate<Object>)o->{replaced.value=now;return true;});check(n==0&&replaced.value==now,"new owner accidentally detached");
  int[] reads={0};var hand=new class_1799();
  Operation<SpellContainer> nativeRead=arguments->{reads[0]++;return new SpellContainer(SpellContainer.ContentType.MAGIC,false,"pool",4,new ArrayList<>(List.of("spell")));};
  var previousFrame=HudSpellReadScope.enter();
  try {
   SpellContainer first=HudSpellReadScope.equipped(hand,player,nativeRead);first.spell_ids.add("modified by caller");
   SpellContainer second=HudSpellReadScope.equipped(hand,player,nativeRead);check(!second.spell_ids.contains("modified by caller")&&reads[0]==1,"HUD result cache leaked mutations or repeated query");
   second.spell_ids.clear();check(HudSpellReadScope.equipped(hand,player,nativeRead).spell_ids.size()==1,"HUD copies share their mutable list");
   var nested=HudSpellReadScope.enter();try{HudSpellReadScope.equipped(hand,player,nativeRead);}finally{HudSpellReadScope.leave(nested);}
   check(reads[0]==2,"nested HUD reused outer query");HudSpellReadScope.equipped(hand,player,nativeRead);check(reads[0]==2,"outer HUD scope not restored");
   HudSpellReadScope.equipped(new class_1799(),player,nativeRead);check(reads[0]==3,"changed stack reused old HUD result");
  }finally{HudSpellReadScope.leave(previousFrame);}
  HudSpellReadScope.equipped(hand,player,nativeRead);HudSpellReadScope.equipped(hand,player,nativeRead);check(reads[0]==5,"HUD cache escaped its frame");
  var frame=HudSpellReadScope.enter();int[] nullReads={0};try {
   Operation<SpellContainer> empty=arguments->{nullReads[0]++;return null;};HudSpellReadScope.equipped(hand,player,empty);HudSpellReadScope.equipped(hand,player,empty);check(nullReads[0]==1,"null spell result repeatedly queried");
  }finally{HudSpellReadScope.leave(frame);}
  frame=HudSpellReadScope.enter();try {
   try{HudSpellReadScope.equipped(hand,player,arguments->{throw new IllegalStateException("fixture");});throw new AssertionError("exception swallowed");}
   catch(IllegalStateException expected){check(true,"expected");}
  }finally{HudSpellReadScope.leave(frame);}
  int before=reads[0];HudSpellReadScope.equipped(hand,player,nativeRead);HudSpellReadScope.equipped(hand,player,nativeRead);check(reads[0]==before+2,"exception left a HUD scope installed");
  System.out.println("Retention production lifecycle checks passed: "+checks);
 }
}
