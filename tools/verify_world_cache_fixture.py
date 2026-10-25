"""Run the production callback against isolated data-only shape fixtures, not a Minecraft JVM."""
from pathlib import Path
import argparse,json,os,subprocess
ROOT=Path(__file__).resolve().parents[1]
p=argparse.ArgumentParser()
for n in ['build-dir','classpath','java-home']:p.add_argument('--'+n,type=Path,required=True)
a=p.parse_args();out=a.build_dir.resolve()/'world-cache-fixture';src=out/'source';classes=out/'classes';classes.mkdir(parents=True,exist_ok=True)
mc={
'class_1937':'public class class_1937 {}',
'class_638':'public class class_638 extends class_1937 {}',
'class_1297':'public class class_1297 { private final class_1937 world; public class_1297(class_1937 w){world=w;} public class_1937 method_37908(){return world;} }',
'class_1309':'public class class_1309 extends class_1297 { public static final ThreadLocal<Object> currentFallEvent=new ThreadLocal<>(); public class_1309(class_1937 w){super(w);} }',
'class_1657':'public class class_1657 extends class_1309 { public class_1657(class_1937 w){super(w);} }',
'class_746':'public class class_746 extends class_1657 { public class_746(class_1937 w){super(w);} }',
'class_1661':'public class class_1661 { public final class_1657 field_7546; public class_1661(class_1657 p){field_7546=p;} }',
'class_2586':'public class class_2586 { private final class_1937 world; public class_2586(class_1937 w){world=w;} public class_1937 method_10997(){return world;} }',
'class_1132':'public class class_1132 {}',
'class_310':'public class class_310 { public class_638 field_1687; public class_746 field_1724; public class_1132 server; public class_1132 method_1576(){return server;} }'}
sources={f'net/minecraft/{n}.java':'package net.minecraft;\n'+v for n,v in mc.items()}
sources.update({
'com/anthonyhilyard/iceberg/util/EntityCollector.java':'''package com.anthonyhilyard.iceberg.util; import java.util.*; import net.minecraft.*; public class EntityCollector extends class_1937 {private final class_1937 wrappedLevel; public static final Map<Object,Object> wrappedLevelsMap=new HashMap<>(); public static Map<Object,Object> entityCache=new HashMap<>(); public EntityCollector(class_1937 w){wrappedLevel=w;}}''',
'com/anthonyhilyard/iceberg/renderer/CustomItemRenderer.java':'''package com.anthonyhilyard.iceberg.renderer; import net.minecraft.*; import com.mojang.datafixers.util.Pair; public class CustomItemRenderer { public static class_1297 armorStand,horse,entity; public static class_2586 blockEntity; public static Pair<?,?> cachedArmorStandItem,cachedHorseArmorItem,cachedEntityItem,cachedBlockEntityItem; }''',
'dev/shadowsoffire/apotheosis/ench/table/ApothEnchantTile.java':'''package dev.shadowsoffire.apotheosis.ench.table; import net.minecraft.*; public class ApothEnchantTile extends class_2586 {public final Object inventory=new Object(){public String toString(){return ApothEnchantTile.this.method_10997().toString();}}; public final Object container=new Object();public ApothEnchantTile(class_1937 w){super(w);}}''',
'net/fabricmc/fabric/impl/transfer/item/InventoryStorageImpl.java':'''package net.fabricmc.fabric.impl.transfer.item; import java.util.*; import com.google.common.collect.MapMaker; public class InventoryStorageImpl { public static final Map<Object,Object> WRAPPERS=new MapMaker().weakValues().makeMap(); }''',
'aztech/modern_industrialization/items/armor/MIKeyMap.java':'''package aztech.modern_industrialization.items.armor; import java.util.*; public class MIKeyMap { public static final Map<Object,Boolean> HOLDING_UP=new HashMap<>(); }''',
'net/blay09/mods/balm/api/event/LivingFallEvent.java':'''package net.blay09.mods.balm.api.event; import net.minecraft.*; public class LivingFallEvent { private final class_1309 entity; public LivingFallEvent(class_1309 e){entity=e;}public class_1309 getEntity(){return entity;} }'''})
for name,text in sources.items():
 f=src/name;f.parent.mkdir(parents=True,exist_ok=True);f.write_text(text,encoding='utf-8')
jar=(a.build_dir/'prominence-korean-compat-1.0.0.jar').resolve();cp=os.pathsep.join([str(jar),*json.loads(a.classpath.read_text('utf-8'))])
params=['--release','17','-proc:none','-encoding','UTF-8','-cp',cp,'-d',str(classes),*map(str,sorted(src.rglob('*.java'))),*map(str,sorted((ROOT/'tests/worldcache').glob('*.java')))];af=out/'compile.args';af.write_text('\n'.join('"'+s.replace('\\','/')+'"' for s in params),encoding='utf-8')
subprocess.run([str(a.java_home/'bin/javac.exe'),'-J-Xmx256m','@'+str(af)],check=True)
r=subprocess.run([str(a.java_home/'bin/java.exe'),'-Xmx256m','-ea','-cp',str(classes)+os.pathsep+cp,'worldcachefixture.FixtureRunner',str(classes),str(jar)],capture_output=True,text=True,encoding='utf-8',errors='replace');(out/'result.log').write_text(r.stdout+r.stderr,encoding='utf-8');assert r.returncode==0,r.stdout+r.stderr;print(r.stdout)
