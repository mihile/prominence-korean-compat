"""Transform the real native client target, then execute native/fixed bodies with API doubles."""
from pathlib import Path
import argparse, json, os, subprocess
ROOT = Path(__file__).resolve().parents[1]

STUBS = {
    'reachverification/Trace.java': '''package reachverification; public class Trace {
        public static double queryRange,hitboxRange,radialRange;public static int attributeReads;
        public static void reset(){queryRange=hitboxRange=radialRange=-999;attributeReads=0;}}''',
    'net/minecraft/class_1320.java': 'package net.minecraft; public class class_1320 {}',
    'net/minecraft/class_1324.java': 'package net.minecraft; public class class_1324 {private final double v;public class_1324(double v){this.v=v;}public double method_6194(){return v;}}',
    'net/minecraft/class_1297.java': 'package net.minecraft; public class class_1297 {public double distance;public class_1297(){}public class_1297(double d){distance=d;}}',
    'net/minecraft/class_1309.java': '''package net.minecraft; public class class_1309 extends class_1297 {
        public Double attackBonus=0.0;public double blockBonus;
        public class_1324 method_5996(class_1320 attribute){reachverification.Trace.attributeReads++;
        if(attribute!=com.jamieswhiteshirt.reachentityattributes.ReachEntityAttributes.ATTACK_RANGE)throw new AssertionError("wrong attribute");
        return attackBonus==null?null:new class_1324(attackBonus);}}''',
    'net/minecraft/class_1657.java': '''package net.minecraft; public class class_1657 extends class_1309 {
        public java.util.List<class_1297> targets=java.util.List.of();public float method_36455(){return 0;}public float method_36454(){return 0;}}''',
    'net/minecraft/class_243.java': 'package net.minecraft; public class class_243 {public final double field_1352,field_1351,field_1350;public class_243(double x,double y,double z){field_1352=x;field_1351=y;field_1350=z;}}',
    'com/jamieswhiteshirt/reachentityattributes/ReachEntityAttributes.java': '''package com.jamieswhiteshirt.reachentityattributes;
        public class ReachEntityAttributes { public static final net.minecraft.class_1320 REACH=new net.minecraft.class_1320(),ATTACK_RANGE=new net.minecraft.class_1320();
        public static double getAttackRange(net.minecraft.class_1309 entity,double base){throw new AssertionError("native API body not installed");}}''',
    'net/bettercombat/api/WeaponAttributes.java': '''package net.bettercombat.api; public class WeaponAttributes {
        public enum HitBoxShape{BOX} public static class Attack{private final double a; public Attack(double a){this.a=a;}
        public double angle(){return a;}public HitBoxShape hitbox(){return HitBoxShape.BOX;}}}''',
    'net/bettercombat/client/collision/WeaponHitBoxes.java': '''package net.bettercombat.client.collision;public class WeaponHitBoxes {
        public static net.minecraft.class_243 createHitbox(net.bettercombat.api.WeaponAttributes.HitBoxShape shape,double range,boolean spin){
        reachverification.Trace.hitboxRange=range;return new net.minecraft.class_243(range,range,range);}}''',
    'net/bettercombat/client/collision/OrientedBoundingBox.java': '''package net.bettercombat.client.collision;public class OrientedBoundingBox {
        public final net.minecraft.class_243 axisZ=new net.minecraft.class_243(0,0,1);public final double range;
        public OrientedBoundingBox(net.minecraft.class_243 origin,net.minecraft.class_243 size,float pitch,float yaw){range=size.field_1350;}
        public OrientedBoundingBox offsetAlongAxisZ(double d){return this;}public OrientedBoundingBox updateVertex(){return this;}}''',
    'net/bettercombat/client/collision/TargetFinder.java': '''package net.bettercombat.client.collision;
        import net.minecraft.*;import java.util.*;import net.bettercombat.api.WeaponAttributes.Attack;
        public class TargetFinder {
        public static TargetResult findAttackTargetResult(class_1657 p,class_1297 e,Attack a,double r){throw new AssertionError("native targeting body not installed");}
        public static class_243 getInitialTracingPoint(class_1657 p){return new class_243(0,0,0);}
        public static List<class_1297> getInitialTargets(class_1657 p,class_1297 cursor,double range){reachverification.Trace.queryRange=range;
            return p.targets.stream().filter(e->e.distance<=range*2+1).toList();}
        public static class CollisionFilter {private final double range;public CollisionFilter(OrientedBoundingBox box){range=box.range;}
            public List<class_1297> filter(List<class_1297> list){return list.stream().filter(e->e.distance<=range).toList();}}
        public static class RadialFilter {private final double range;public RadialFilter(class_243 a,class_243 b,double range,double angle){this.range=range;reachverification.Trace.radialRange=range;}
            public List<class_1297> filter(List<class_1297> list){return list.stream().filter(e->e.distance<=range).toList();}}
        public static class TargetResult {public final List<class_1297> entities;public final OrientedBoundingBox hitbox;
            public TargetResult(List<class_1297> entities,OrientedBoundingBox hitbox){this.entities=entities;this.hitbox=hitbox;}}
        }''',
    'net/fabricmc/loader/api/Version.java': 'package net.fabricmc.loader.api;public interface Version {String getFriendlyString();}',
    'net/fabricmc/loader/api/metadata/ModMetadata.java': 'package net.fabricmc.loader.api.metadata;public interface ModMetadata {net.fabricmc.loader.api.Version getVersion();}',
    'net/fabricmc/loader/api/ModContainer.java': 'package net.fabricmc.loader.api;public interface ModContainer {net.fabricmc.loader.api.metadata.ModMetadata getMetadata();java.util.Optional<java.nio.file.Path> findPath(String path);}',
    'net/fabricmc/loader/api/FabricLoader.java': '''package net.fabricmc.loader.api;public interface FabricLoader {
        static FabricLoader getInstance(){return Holder.INSTANCE;}java.util.Optional<ModContainer> getModContainer(String id);
        class Holder {public static FabricLoader INSTANCE=id->java.util.Optional.empty();}}'''
}


def main():
    parser = argparse.ArgumentParser()
    for name in ['build-dir', 'classpath', 'java-home']:
        parser.add_argument('--' + name, type=Path, required=True)
    args = parser.parse_args()
    build = args.build_dir.resolve()
    work = build / 'native-reach'
    transform = work / 'transform-classes'
    fixture = work / 'fixture-classes'
    transform.mkdir(parents=True, exist_ok=True)
    fixture.mkdir(parents=True, exist_ok=True)
    cp = [str(build / 'prominence-korean-compat-1.0.0.jar'), *json.loads(args.classpath.read_text('utf8'))]
    java, javac = [args.java_home / ('bin/' + name + '.exe') for name in ['java', 'javac']]

    def run(executable, values, label):
        argfile = work / (label + '.args')
        jvm = [str(s) for s in values if str(s).startswith('-J')]
        options = [s for s in values if not str(s).startswith('-J')]
        argfile.write_text('\n'.join('"' + str(s).replace('\\', '/') + '"' for s in options), 'utf8')
        result = subprocess.run([str(executable), *jvm, '@' + str(argfile)], capture_output=True, text=True, encoding='utf8', errors='replace')
        (work / (label + '.log')).write_text(result.stdout + result.stderr, 'utf8')
        assert result.returncode == 0, result.stdout + result.stderr
        print(result.stdout.strip())

    sources = [ROOT / 'tests/native/batchverification/VerifyMixinTransforms.java',
               ROOT / 'tests/native/reachverification/VerifyReachTransform.java']
    run(javac, ['-J-Xmx256m', '--release', '17', '-encoding', 'UTF-8', '-proc:none', '-cp', os.pathsep.join(cp), '-d', transform, *sources], 'compile-transform')
    services = transform / 'META-INF/services'
    services.mkdir(parents=True, exist_ok=True)
    for interface, implementation in [('IMixinService', 'batchverification.VerifyMixinTransforms'),
                                       ('IMixinServiceBootstrap', 'batchverification.VerifyMixinTransforms$Bootstrap'),
                                       ('IGlobalPropertyService', 'batchverification.VerifyMixinTransforms$Props')]:
        (services / ('org.spongepowered.asm.service.' + interface)).write_text(implementation + '\n', 'utf8')
    run(java, ['-Xmx384m', '-Xverify:all', '-cp', os.pathsep.join([str(transform), *cp]),
               'reachverification.VerifyReachTransform', work, ROOT / 'src/main/resources/prominence-combat-reach-pins.json'], 'transform')
    stub_sources = []
    for name, source in STUBS.items():
        path = work / 'stubs' / name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(source, 'utf8')
        stub_sources.append(path)
    sources = [ROOT / ('tests/native/reachverification/' + name + '.java') for name in
               ['PrepareReachFixture', 'ReachBehaviorChecks', 'ReachGuardChecks']]
    run(javac, ['-J-Xmx256m', '--release', '17', '-encoding', 'UTF-8', '-proc:none', '-cp', os.pathsep.join(cp),
                '-d', fixture, *stub_sources, *sources], 'compile-fixture')
    fixture_cp = [str(fixture), *cp]
    run(java, ['-Xmx128m', '-cp', os.pathsep.join(fixture_cp), 'reachverification.PrepareReachFixture', work, fixture], 'prepare-fixture')
    for mode in ['native', 'fixed']:
        runtime = [str(work / (mode + '-fixture')), *fixture_cp]
        run(java, ['-Xmx128m', '-Xverify:all', '-cp', os.pathsep.join(runtime), 'reachverification.ReachBehaviorChecks', mode], mode + '-behavior')
    run(java, ['-Xmx128m', '-Xverify:all', '-cp', os.pathsep.join(fixture_cp), 'reachverification.ReachGuardChecks', work / 'pins'], 'guard')
    (work / 'report.json').write_text(json.dumps({'status': 'passed', 'nativeMixin': True, 'nativeTargetingAndReachApiBodies': True,
                                                'worldAndGeometry': 'API doubles', 'gameStarted': False}, indent=2), 'utf8')


if __name__ == '__main__':
    main()
