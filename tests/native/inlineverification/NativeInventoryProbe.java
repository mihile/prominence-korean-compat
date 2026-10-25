import io.github.fabricators_of_create.porting_lib.transfer.item.ItemStackHandler;
import net.minecraft.*;
/** Data-only native registry/inventory fixture. No Minecraft main, world, client, server or GL. */
public class NativeInventoryProbe {
 static final java.io.PrintStream REPORT=System.out;
 public static void main(String[] args){
  var diagnostic=System.err;try{run();}catch(Throwable t){t.printStackTrace(diagnostic);System.exit(1);}
 }
 static void run()throws Exception{
  // Only supplies the dependency's environment query; no launcher or game is started.
  net.fabricmc.loader.impl.launch.FabricLauncherBase.setLauncher((net.fabricmc.loader.impl.launch.FabricLauncher)java.lang.reflect.Proxy.newProxyInstance(NativeInventoryProbe.class.getClassLoader(),new Class[]{net.fabricmc.loader.impl.launch.FabricLauncher.class},(p,m,a)->{
   if(m.getName().equals("isDevelopment"))return false;
   throw new UnsupportedOperationException("Unexpected launcher call: "+m.getName());
  }));
  class_155.method_36208();class_2966.method_12851();
  var h=new ItemStackHandler(2);
  var original=new class_1799(class_1802.field_8322,3); // Item identity is irrelevant to NBT persistence.
  var gear=new class_1799(class_1802.field_8322,1);
  gear.method_7948().method_10582("fixture","enchants-and-custom-NBT");
  h.setStackInSlot(0,original);h.setSize(5);h.setStackInSlot(0,original);
  h.setStackInSlot(2,new class_1799(class_1802.field_8322,12));h.setStackInSlot(3,gear);h.setStackInSlot(4,new class_1799(class_1802.field_8322,9));
  var saved=h.serializeNBT();var oldPeer=new ItemStackHandler(2);oldPeer.deserializeNBT(saved);
  if(oldPeer.getSlotCount()!=5||oldPeer.getStackInSlot(0).method_7947()!=3||oldPeer.getStackInSlot(2).method_7947()!=12||oldPeer.getStackInSlot(4).method_7947()!=9)throw new AssertionError("Five-cell round trip");
  if(!oldPeer.getStackInSlot(3).method_7948().method_10558("fixture").equals("enchants-and-custom-NBT"))throw new AssertionError("Gear metadata lost");
  var live=h.getStackInSlot(3);live.method_7948().method_10582("quality","new");h.setStackInSlot(3,live);
  if(!h.getStackInSlot(3).method_7948().method_10558("quality").equals("new"))throw new AssertionError();
  oldPeer.deserializeNBT(h.serializeNBT());if(!oldPeer.getStackInSlot(3).method_7948().method_10558("quality").equals("new"))throw new AssertionError("Mutation lost");
  // Exercise the production adapter against real native classes without constructing a player/world/menu.
  var unsafeField=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");unsafeField.setAccessible(true);var unsafe=(sun.misc.Unsafe)unsafeField.get(null);
  var owner=(net.p3pp3rf1y.sophisticatedbackpacks.upgrades.anvil.AnvilUpgradeContainer)unsafe.allocateInstance(net.p3pp3rf1y.sophisticatedbackpacks.upgrades.anvil.AnvilUpgradeContainer.class);
  var wrapper=(net.p3pp3rf1y.sophisticatedbackpacks.upgrades.anvil.AnvilUpgradeWrapper)unsafe.allocateInstance(net.p3pp3rf1y.sophisticatedbackpacks.upgrades.anvil.AnvilUpgradeWrapper.class);
  set(wrapper,"inventory",h);set(owner,"upgradeWrapper",wrapper);
  var state=(local.prominence.backpackinline.InlineState)unsafe.allocateInstance(local.prominence.backpackinline.InlineState.class);
  set(state,"owner",owner);set(state,"batching",true); // Suppress world validation; test only production inventory operations.
  var ctor=local.prominence.backpackinline.InlineInventory.class.getDeclaredConstructor(local.prominence.backpackinline.InlineState.class);ctor.setAccessible(true);var adapter=ctor.newInstance(state);
  if(adapter.method_5438(1)!=h.getStackInSlot(3))throw new AssertionError("Not canonical gear reference");
  var removed=adapter.method_5434(0,4);if(removed.method_7947()!=4||h.getStackInSlot(2).method_7947()!=8||h.getStackInSlot(0).method_7947()!=3)throw new AssertionError("Split moved wrong cells or duplicated");
  var added=adapter.method_5441(2);if(added.method_7947()!=9||!adapter.method_5438(2).method_7960())throw new AssertionError("Remove all");
  adapter.method_5447(2,added);adapter.method_5431();oldPeer.deserializeNBT(h.serializeNBT());
  if(oldPeer.getStackInSlot(4).method_7947()!=9||!oldPeer.getStackInSlot(3).method_7948().method_10558("quality").equals("new"))throw new AssertionError("Adapter save lost data");
  adapter.method_5448();if(!adapter.method_5442()||h.getStackInSlot(0).method_7947()!=3)throw new AssertionError("Clear affected original anvil inputs");
  REPORT.println("PASS native inventory persistence: expanded five cells survive old two-cell constructor, custom gear NBT and in-place mutation survive save/reopen");
  REPORT.println("PASS production inline adapter: canonical references, offset cells, split/removal counts, markDirty save and clear preserve native anvil inputs; no player/world created");
 }
 static void set(Object o,String name,Object value)throws Exception{Class<?> t=o.getClass();while(t!=null){try{var f=t.getDeclaredField(name);f.setAccessible(true);f.set(o,value);return;}catch(NoSuchFieldException ignored){t=t.getSuperclass();}}throw new NoSuchFieldException(name);}
}
