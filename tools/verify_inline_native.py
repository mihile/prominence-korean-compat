"""CPU-only native Mixin transformation fixture; never launches a game or creates a world."""
from pathlib import Path
import argparse,json,os,subprocess
p=argparse.ArgumentParser()
for n in ['build-dir','classpath','java-home']:p.add_argument('--'+n,type=Path,required=True)
a=p.parse_args();repo=Path(__file__).resolve().parents[1];out=a.build_dir/'native-inline';classes=out/'classes';classes.mkdir(parents=True,exist_ok=True)
cp=[str((a.build_dir/'prominence-korean-compat-1.0.0.jar').resolve()),*json.loads(a.classpath.read_text('utf8'))]
def run(args,log):
 r=subprocess.run(list(map(str,args)),capture_output=True,text=True,encoding='utf8',errors='replace');(out/log).write_text(r.stdout+r.stderr,encoding='utf8');assert r.returncode==0,r.stdout+r.stderr;print(r.stdout[-2200:])
params=['--release','17','-encoding','UTF-8','-proc:none','-cp',os.pathsep.join(cp),'-d',str(classes),str(repo/'tests/native/batchverification/VerifyMixinTransforms.java'),str(repo/'tests/native/inlineverification/VerifyInlineTransforms.java')]
af=out/'compile.args';af.write_text('\n'.join('"'+s.replace('\\','/')+'"' for s in params),encoding='utf8');run([a.java_home/'bin/javac.exe','@'+str(af)],'compile.log')
services=classes/'META-INF/services';services.mkdir(exist_ok=True,parents=True)
for name,impl in {'org.spongepowered.asm.service.IMixinService':'batchverification.VerifyMixinTransforms','org.spongepowered.asm.service.IMixinServiceBootstrap':'batchverification.VerifyMixinTransforms$Bootstrap','org.spongepowered.asm.service.IGlobalPropertyService':'batchverification.VerifyMixinTransforms$Props'}.items():(services/name).write_text(impl+'\n',encoding='utf8')
run([a.java_home/'bin/java.exe','-Xmx768m','-ea','-cp',os.pathsep.join([str(classes),*cp]),'inlineverification.VerifyInlineTransforms',repo/'src/main/resources',out/'verification'],'transform.log')
