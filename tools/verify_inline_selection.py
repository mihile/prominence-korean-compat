"""Replay native tab-selection bodies with production receiver/state; no Minecraft/world/GL."""
from pathlib import Path
import argparse, json, os, subprocess
ROOT=Path(__file__).resolve().parents[1]
p=argparse.ArgumentParser()
for n in ['jar','core','classpath','java-home','output']:p.add_argument('--'+n,type=Path,required=True)
p.add_argument('--baseline',action='store_true',help='Reproduce both stale-flag failures with the previous public JAR')
a=p.parse_args();out=a.output.resolve();src=out/'src';classes=out/'classes';classes.mkdir(parents=True,exist_ok=True)
sources={
'net/minecraft/class_2960.java':'public class class_2960 {public class_2960(String a,String b){}}',
'net/minecraft/class_2487.java':'public class class_2487 {public class_2487 method_10562(String s){return this;}public void method_10569(String k,int v){}}',
'net/minecraft/class_1937.java':'public class class_1937 {public boolean field_9236;public long tick;public long method_8510(){return tick;}}',
'net/minecraft/class_1735.java':'public class class_1735 {}',
'net/minecraft/class_1799.java':'public class class_1799 {public static final class_1799 field_8037=new class_1799();public final class_2487 nbt=new class_2487();public class_2487 method_7948(){return nbt;}}',
'net/minecraft/class_1263.java':'public interface class_1263 {}',
'net/minecraft/class_1703.java':'public class class_1703 {public int field_7763=42;}',
'net/minecraft/class_1657.java':'public class class_1657 {public class_1703 field_7512;public final class_1937 world=new class_1937();public class_1937 method_37908(){return world;}}',
'net/minecraft/class_3244.java':'''public class class_3244 implements local.prominence.backpackinline.HandshakeAccess {public boolean agreed=true;public boolean prominence$inlineAgreed(){return agreed;}public void prominence$inlineAgree(){agreed=true;}}''',
'net/minecraft/class_3222.java':'public class class_3222 extends class_1657 {public class_3244 field_13987=new class_3244();}',
'net/minecraft/class_2540.java':'''public class class_2540 {private final io.netty.buffer.ByteBuf b;public class_2540(io.netty.buffer.ByteBuf b){this.b=b;}public int readableBytes(){return b.readableBytes();}public int readInt(){return b.readInt();}public boolean readBoolean(){return b.readBoolean();}public io.netty.buffer.ByteBuf writeInt(int n){return b.writeInt(n);}public io.netty.buffer.ByteBuf writeBoolean(boolean n){return b.writeBoolean(n);}public void release(){b.release();}}''',
'net/minecraft/server/MinecraftServer.java':'''package net.minecraft.server;public class MinecraftServer {public void execute(Runnable r){r.run();}}''',
'net/fabricmc/fabric/api/networking/v1/PacketSender.java':'''package net.fabricmc.fabric.api.networking.v1;public interface PacketSender {}''',
'net/fabricmc/fabric/api/networking/v1/ServerPlayNetworking.java':'''package net.fabricmc.fabric.api.networking.v1;
import net.minecraft.*;import net.minecraft.server.MinecraftServer;public class ServerPlayNetworking {
public interface PlayChannelHandler {void receive(MinecraftServer s,class_3222 p,class_3244 n,class_2540 b,PacketSender sender);}
public static PlayChannelHandler receiver;public static boolean supported=true;public static int sent;public static boolean lastMode,lastReady;
public static boolean canSend(class_3222 p,class_2960 id){return supported;}
public static boolean registerGlobalReceiver(class_2960 id,PlayChannelHandler h){receiver=h;return true;}
public static void send(class_3222 p,class_2960 id,class_2540 b){b.readInt();b.readInt();lastMode=b.readBoolean();lastReady=b.readBoolean();sent++;b.release();}}
''',
'net/p3pp3rf1y/sophisticatedcore/api/IStorageWrapper.java':'''package net.p3pp3rf1y.sophisticatedcore.api;import java.util.Optional;public interface IStorageWrapper {Optional<Integer> getOpenTabId();void setOpenTabId(int n);void removeOpenTabId();}''',
'net/p3pp3rf1y/sophisticatedcore/common/gui/StorageContainerMenuBase.java':'''package net.p3pp3rf1y.sophisticatedcore.common.gui;
import java.util.*;import java.util.function.*;import net.minecraft.*;import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
public class StorageContainerMenuBase<S extends IStorageWrapper> extends class_1703 {
public final Map<Integer,UpgradeContainerBase<?,?>> upgradeContainers=new HashMap<>();public IStorageWrapper storageWrapper;
public StorageContainerMenuBase(){storageWrapper=new IStorageWrapper(){Optional<Integer> selected=Optional.empty();public Optional<Integer> getOpenTabId(){return selected;}public void setOpenTabId(int n){selected=Optional.of(n);}public void removeOpenTabId(){selected=Optional.empty();}};}
public Map<Integer,UpgradeContainerBase<?,?>> getUpgradeContainers(){return upgradeContainers;}
public void setOpenTabId(int n){throw new AssertionError("Native method not installed");}
public Optional<UpgradeContainerBase<?,?>> getOpenContainer(){throw new AssertionError("Native method not installed");}
protected boolean isClientSide(){return false;}protected void sendToServer(Consumer<class_2487> c){throw new AssertionError("Unexpected client branch");}
public int getTotalSlotsNumber(){return upgradeContainers.values().stream().mapToInt(c->c.getSlots().size()).sum();}
public class_1735 method_7611(int n){return upgradeContainers.values().stream().flatMap(c->c.getSlots().stream()).toList().get(n);}}
''',
'net/p3pp3rf1y/sophisticatedcore/common/gui/UpgradeContainerBase.java':'''package net.p3pp3rf1y.sophisticatedcore.common.gui;
import java.util.*;import net.minecraft.*;public class UpgradeContainerBase<W,C> {
private boolean isOpen;public final List<class_1735> slots=new ArrayList<>();public int id;public UpgradeContainerBase(int id){this.id=id;for(int i=0;i<3;i++)slots.add(new class_1735());}
public boolean isOpen(){throw new AssertionError("Native method not installed");}public void setIsOpen(boolean b){throw new AssertionError("Native method not installed");}
public int getUpgradeContainerId(){return id;}public List<class_1735> getSlots(){return slots;}public boolean containsSlot(class_1735 s){return slots.contains(s);}}
''',
'net/p3pp3rf1y/sophisticatedbackpacks/common/gui/BackpackContainer.java':'''package net.p3pp3rf1y.sophisticatedbackpacks.common.gui;
import net.minecraft.*;import net.p3pp3rf1y.sophisticatedcore.common.gui.*;import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
public class BackpackContainer extends StorageContainerMenuBase<IStorageWrapper>{public boolean accessible=true;public boolean method_7597(class_1657 p){return accessible;}}
''',
'net/p3pp3rf1y/sophisticatedbackpacks/upgrades/anvil/AnvilUpgradeWrapper.java':'''package net.p3pp3rf1y.sophisticatedbackpacks.upgrades.anvil;
public class AnvilUpgradeWrapper {public Handler getInventory(){return new Handler();}public static class Handler {public int getSlotCount(){return 5;}public Object getStackInSlot(int i){return null;}public void setSize(int s){}public void setStackInSlot(int i,Object o){}}}
''',
'net/p3pp3rf1y/sophisticatedbackpacks/upgrades/anvil/AnvilUpgradeContainer.java':'''package net.p3pp3rf1y.sophisticatedbackpacks.upgrades.anvil;
import local.prominence.backpackinline.*;import net.minecraft.*;import net.p3pp3rf1y.sophisticatedcore.common.gui.*;
public class AnvilUpgradeContainer extends UpgradeContainerBase<AnvilUpgradeWrapper,Object> implements InlineAccess {
public InlineState state;public final Stack stack=new Stack();public AnvilUpgradeContainer(int id){super(id);}public InlineState prominence$inline(){return state;}
public AnvilUpgradeWrapper getUpgradeWrapper(){return new AnvilUpgradeWrapper();}public class_1799 getUpgradeStack(){return stack;}
public static class Stack extends class_1799 {}}
''',
'local/prominence/backpackinline/InlineGuard.java':'''package local.prominence.backpackinline;public class InlineGuard {public static boolean enabled(){return true;}}''',
'local/prominence/backpackinline/InlineInventory.java':'''package local.prominence.backpackinline;import net.minecraft.*;public class InlineInventory implements class_1263 {public InlineInventory(InlineState s){}public class_1799 method_5438(int n){return class_1799.field_8037;}public void method_5431(){}}''',
'local/prominence/backpackinline/InlineDelegate.java':'''package local.prominence.backpackinline;import net.minecraft.*;import local.prominence.backpackinline.mixin.ReforgeInventoryAccess;
public class InlineDelegate implements ReforgeInventoryAccess {public static int forged,refreshed;public InlineDelegate(InlineState s){}public class_1735 method_7611(int i){return new class_1735();}public void method_7609(class_1263 i){refreshed++;}public void reforge(){forged++;}public void prominence$inventory(class_1263 i){}public boolean prominence$ready(){return true;}}''',
'elocindev/tierify/screen/ReforgeScreenHandler.java':'''package elocindev.tierify.screen;import net.minecraft.*;public class ReforgeScreenHandler {public static boolean isValidAddition(class_1799 s){return true;}}'''
}
for name,text in sources.items():
    if name.startswith('net/minecraft/') and not text.startswith('package '):text='package net.minecraft;'+text
    path=src/name;path.parent.mkdir(parents=True,exist_ok=True);path.write_text(text,encoding='utf-8')
cp=os.pathsep.join([str(a.jar.resolve()),*json.loads(a.classpath.read_text('utf-8'))])
params=['--release','17','-encoding','UTF-8','-proc:none','-cp',cp,'-d',str(classes),*map(str,sorted(src.rglob('*.java'))),*map(str,sorted((ROOT/'tests/inlineselection').glob('*.java')))];af=out/'compile.args';af.write_text('\n'.join('"'+s.replace('\\','/')+'"' for s in params),encoding='utf-8')
subprocess.run([str(a.java_home/'bin/javac.exe'),'@'+str(af)],check=True)
r=subprocess.run([str(a.java_home/'bin/java.exe'),'-Xmx256m','-ea','-cp',str(classes)+os.pathsep+cp,'inlineselection.NativeSelectionFixture',str(classes),str(a.jar.resolve()),str(a.core.resolve()),'baseline' if a.baseline else 'fixed'],capture_output=True,text=True,encoding='utf-8',errors='replace');(out/'result.log').write_text(r.stdout+r.stderr,encoding='utf-8');assert r.returncode==0,r.stdout+r.stderr;print(r.stdout)
