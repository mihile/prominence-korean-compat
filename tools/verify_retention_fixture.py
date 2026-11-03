"""Execute the built production cleanup on isolated data shapes; no Minecraft/server/world runtime."""
from pathlib import Path
import argparse,json,subprocess,os,ctypes
p=argparse.ArgumentParser()
for n in ['build-dir','classpath','java-home']:p.add_argument('--'+n,type=Path,required=True)
a=p.parse_args();repo=Path(__file__).resolve().parents[1];out=a.build_dir/'retention-fixture';src=out/'source';classes=out/'classes';classes.mkdir(parents=True,exist_ok=True)
def short(path):
 path=str(Path(path).resolve());buf=ctypes.create_unicode_buffer(32768)
 return buf.value if ctypes.windll.kernel32.GetShortPathNameW(path,buf,len(buf)) else path
types={
'class_1937':'public class class_1937 {}',
'class_638':'public class class_638 extends class_1937 {}',
'class_1297':'public class class_1297 {private final class_1937 world;public class_1297(class_1937 w){world=w;}public class_1937 method_37908(){return world;}}',
'class_1657':'public class class_1657 extends class_1297 {public class_1657(class_1937 w){super(w);}}',
'class_746':'public class class_746 extends class_1657 {public class_746(class_1937 w){super(w);}}',
'class_634':'public class class_634 {}',
'class_2535':'public class class_2535 {public boolean open;public class_2535(boolean open){this.open=open;}public boolean method_10758(){return open;}}',
'class_1799':'public class class_1799 {public static final class_1799 field_8037=new class_1799();}',
'class_1706':'public class class_1706 {public static dev.shadowsoffire.apotheosis.util.Events.RepairEvent zenith$event;public static class_1799 zenith$leftItem,zenith$rightItem,zenith$output;}',
'class_1703':'public class class_1703 {}',
'class_1723':'public class class_1723 extends class_1703 {private final class_1657 field_7828;public class_1723(class_1657 p){field_7828=p;}}',
'class_437':'public class class_437 {}',
'class_490':'public class class_490 extends class_437 {class_1703 handler;public class_490(class_1703 h){handler=h;}public class_1703 method_17577(){return handler;}}',
'class_1132':'public class class_1132 {}',
'class_310':'public class class_310 {public class_638 field_1687;public class_746 field_1724;public class_634 connection;public class_1132 server;public class_634 method_1562(){return connection;}public class_1132 method_1576(){return server;}}'}
sources={f'net/minecraft/{n}.java':'package net.minecraft;\n'+s for n,s in types.items()}
sources.update({
'lol/bai/badpackets/impl/registry/ChannelRegistry.java':'package lol.bai.badpackets.impl.registry;import java.util.*;public class ChannelRegistry {public static final ChannelRegistry S2C=new ChannelRegistry();public final Set<Object> handlers=new HashSet<>();}',
'lol/bai/badpackets/impl/handler/AbstractPacketHandler.java':'package lol.bai.badpackets.impl.handler;import net.minecraft.*;public class AbstractPacketHandler {private final class_2535 connection;public AbstractPacketHandler(class_2535 c){connection=c;}}',
'lol/bai/badpackets/impl/handler/ClientPacketHandler.java':'package lol.bai.badpackets.impl.handler;import net.minecraft.*;import lol.bai.badpackets.impl.registry.*;public class ClientPacketHandler extends AbstractPacketHandler {private final class_634 listener;public ClientPacketHandler(class_634 n,class_2535 c){super(c);listener=n;ChannelRegistry.S2C.handlers.add(this);}}',
'de/mari_023/ae2wtlib/wct/CraftingTerminalHandler.java':'package de.mari_023.ae2wtlib.wct;import java.util.*;import net.minecraft.*;public class CraftingTerminalHandler {private static final WeakHashMap<class_1657,CraftingTerminalHandler> players=new WeakHashMap<>();private final class_1657 player;public CraftingTerminalHandler(class_1657 p){player=p;players.put(p,this);}public static Map<class_1657,CraftingTerminalHandler> players(){return players;}}',
'com/anthonyhilyard/legendarytooltips/config/LegendaryTooltipsConfig.java':'package com.anthonyhilyard.legendarytooltips.config;import java.util.*;public class LegendaryTooltipsConfig {private static final Map<Object,Object> frameDefinitionCache=new HashMap<>();public static final List<String> colors=List.of("keep");public static Map<Object,Object> cache(){return frameDefinitionCache;}}',
'net/p3pp3rf1y/sophisticatedcore/inventory/ItemStackKey.java':'package net.p3pp3rf1y.sophisticatedcore.inventory;import java.util.*;public class ItemStackKey {private static final Map<Object,Object> CACHE=new java.util.concurrent.ConcurrentHashMap<>();public static Map<Object,Object> cache(){return CACHE;}}',
'dev/shadowsoffire/apotheosis/util/Events.java':'package dev.shadowsoffire.apotheosis.util;import net.minecraft.*;public class Events {public static class RepairEvent {public final class_1657 player;public RepairEvent(class_1657 p){player=p;}}}',
'mod/azure/azurelibarmor/core/molang/LazyVariable.java':'package mod.azure.azurelibarmor.core.molang;import java.util.function.*;public class LazyVariable {private DoubleSupplier valueSupplier;public LazyVariable(DoubleSupplier s){valueSupplier=s;}public double get(){return valueSupplier.getAsDouble();}}',
'mod/azure/azurelibarmor/core/molang/MolangParser.java':'package mod.azure.azurelibarmor.core.molang;import java.util.*;import java.util.function.*;public class MolangParser {public static final Map<String,LazyVariable> VARIABLES=new HashMap<>();public static void setMemoizedValue(String name,DoubleSupplier value){VARIABLES.put(name,new LazyVariable(new DoubleSupplier(){public double getAsDouble(){return value.getAsDouble();}}));}}',
'mod/azure/azurelibarmor/model/GeoModel.java':'package mod.azure.azurelibarmor.model;import mod.azure.azurelibarmor.core.molang.*;public class GeoModel {public static void query(String name,Object owner){MolangParser.setMemoizedValue(name,()->owner.hashCode());}}',
'retentionfixture/Runner.java':'''package retentionfixture;import java.net.*;import java.nio.file.*;public class Runner {public static void main(String[]args)throws Exception {try(var l=new URLClassLoader(new URL[]{Path.of(args[0]).toUri().toURL(),Path.of(args[1]).toUri().toURL()},Runner.class.getClassLoader()){protected Class<?> loadClass(String n,boolean resolve)throws ClassNotFoundException {synchronized(getClassLoadingLock(n)){Class<?> c=findLoadedClass(n);boolean own=n.startsWith("net.minecraft.")||n.equals("retentionfixture.Probe")||n.startsWith("retentionfixture.Probe$")||n.startsWith("local.prominence.memory.")||n.startsWith("lol.bai.badpackets.")||n.startsWith("de.mari_023.ae2wtlib.")||n.startsWith("com.anthonyhilyard.legendarytooltips.")||n.startsWith("net.p3pp3rf1y.sophisticatedcore.inventory.")||n.startsWith("dev.shadowsoffire.apotheosis.")||n.startsWith("mod.azure.azurelibarmor.");if(c==null&&own)try{c=findClass(n);}catch(ClassNotFoundException e){}if(c==null)c=super.loadClass(n,false);if(resolve)resolveClass(c);return c;}}}){l.loadClass("retentionfixture.Probe").getMethod("main",String[].class).invoke(null,(Object)new String[0]);}}}'''})
for name,text in sources.items():
 f=src/name;f.parent.mkdir(parents=True,exist_ok=True);f.write_text(text,'utf8')
cp=[str((a.build_dir/'prominence-korean-compat-1.0.0.jar').resolve()),*json.loads(a.classpath.read_text('utf8'))]
def run(args,name):
 r=subprocess.run(list(map(str,args)),capture_output=True,text=True,encoding='utf8',errors='replace');(out/name).write_text(r.stdout+r.stderr,'utf8');assert r.returncode==0,r.stdout+r.stderr;print(r.stdout[-2000:])
args=['--release','17','-proc:none','-encoding','UTF-8','-cp',os.pathsep.join(cp),'-d',str(classes),*map(str,src.rglob('*.java')),str(repo/'tests/retention/Probe.java')]
f=out/'compile.args';f.write_text('\n'.join('"'+s.replace('\\','/')+'"' for s in args),'utf8');run([a.java_home/'bin/javac.exe','@'+str(f)],'compile.log')
args=['-Xmx256m','-ea','-cp',os.pathsep.join(short(p) for p in [classes,*cp]),'retentionfixture.Runner',short(classes),short(cp[0])]
f=out/'runtime.args';f.write_text('\n'.join('"'+s.replace('\\','/')+'"' for s in args),'utf8');run([a.java_home/'bin/java.exe','@'+str(f)],'fixture.log')
