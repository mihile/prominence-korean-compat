"""Transform real Xaero targets, then run the production scope with a data-only world stand-in."""
from pathlib import Path
import argparse,json,os,subprocess
ROOT=Path(__file__).resolve().parents[1];p=argparse.ArgumentParser()
for n in ['build-dir','classpath','java-home']:p.add_argument('--'+n,type=Path,required=True)
a=p.parse_args();out=a.build_dir.resolve()/'native-xaero-biome';classes=out/'classes';classes.mkdir(parents=True,exist_ok=True)
cp=[str((a.build_dir/'prominence-korean-compat-1.0.0.jar').resolve()),*json.loads(a.classpath.read_text('utf-8'))]
def compile(files,extra=[]):
    params=['--release','17','-encoding','UTF-8','-proc:none','-cp',os.pathsep.join([*extra,*cp]),'-d',str(classes),*map(str,files)];af=out/'compile.args';af.write_text('\n'.join('"'+s.replace('\\','/')+'"' for s in params),encoding='utf-8');subprocess.run([str(a.java_home/'bin/javac.exe'),'@'+str(af)],check=True)
def run(entry,args,log):
    r=subprocess.run([str(a.java_home/'bin/java.exe'),'-Xmx384m','-ea','-cp',os.pathsep.join([str(classes),*cp]),entry,*map(str,args)],cwd=out,capture_output=True,text=True,encoding='utf-8',errors='replace');(out/log).write_text(r.stdout+r.stderr,encoding='utf-8');assert r.returncode==0,r.stdout+r.stderr;print(r.stdout[-2200:])
compile([ROOT/'tests/native/batchverification/VerifyMixinTransforms.java',ROOT/'tests/native/xaerobiome/VerifyXaeroBiome.java'])
services=classes/'META-INF/services';services.mkdir(parents=True,exist_ok=True)
for name,impl in {'IMixinService':'batchverification.VerifyMixinTransforms','IMixinServiceBootstrap':'batchverification.VerifyMixinTransforms$Bootstrap','IGlobalPropertyService':'batchverification.VerifyMixinTransforms$Props'}.items():(services/('org.spongepowered.asm.service.'+name)).write_text(impl+'\n',encoding='utf-8')
run('xaerobiome.VerifyXaeroBiome',[ROOT/'src/main/resources',out/'verification'],'transform.log')
stub=out/'stub/net/minecraft/class_1937.java';stub.parent.mkdir(parents=True,exist_ok=True);stub.write_text('package net.minecraft;public class class_1937 {private final int id;public class_1937(int id){this.id=id;}public int method_31605(){return id;}}',encoding='utf-8')
compile([stub,ROOT/'tests/native/xaerobiome/ScopeProbe.java',ROOT/'tests/native/xaerobiome/ScopeRunner.java'])
run('xaerobiome.ScopeRunner',[classes,out/'verification/transformed',(a.build_dir/'prominence-korean-compat-1.0.0.jar').resolve()],'scope.log')
