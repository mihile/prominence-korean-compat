"""Native FTB transformations plus real Caramel composition. Test Main replaces JNI only, never ships."""
from pathlib import Path
import argparse,json,os,subprocess,zipfile
p=argparse.ArgumentParser()
for n in ['build-dir','classpath','java-home']:p.add_argument('--'+n,type=Path,required=True)
a=p.parse_args();repo=Path(__file__).resolve().parents[1];out=a.build_dir/'ftb-ime-verification';classes=out/'classes';classes.mkdir(parents=True,exist_ok=True)
jar=(a.build_dir/'prominence-korean-compat-1.0.0.jar').resolve()
with zipfile.ZipFile(jar) as z:assert not any(n.startswith('moe/caramel/') or n.startswith('dev/ftb/') for n in z.namelist()),'Never redistribute native mod classes or test facades'
cp=[str(jar),*json.loads(a.classpath.read_text('utf8'))]
def run(args,log):
 r=subprocess.run(list(map(str,args)),capture_output=True,text=True,encoding='utf8',errors='replace');(out/log).write_text(r.stdout+r.stderr,encoding='utf8');assert r.returncode==0,r.stdout+r.stderr;print(r.stdout[-4000:])
sources=[repo/'tests/native/batchverification/VerifyMixinTransforms.java',repo/'tests/native/inlineverification/VerifyFtbImeTransforms.java',*sorted((repo/'tests/native/imefixture').rglob('*.java'))]
params=['--release','17','-encoding','UTF-8','-proc:none','-cp',os.pathsep.join(cp),'-d',str(classes),*map(str,sources)]
af=out/'compile.args';af.write_text('\n'.join('"'+s.replace('\\','/')+'"' for s in params),encoding='utf8');run([a.java_home/'bin/javac.exe','@'+str(af)],'compile.log')
services=classes/'META-INF/services';services.mkdir(exist_ok=True,parents=True)
for name,impl in {'org.spongepowered.asm.service.IMixinService':'batchverification.VerifyMixinTransforms','org.spongepowered.asm.service.IMixinServiceBootstrap':'batchverification.VerifyMixinTransforms$Bootstrap','org.spongepowered.asm.service.IGlobalPropertyService':'batchverification.VerifyMixinTransforms$Props'}.items():(services/name).write_text(impl+'\n',encoding='utf8')
runtime=os.pathsep.join([str(classes),*cp])
run([a.java_home/'bin/java.exe','-Xmx512m','-ea','-cp',runtime,'imefixture.ImeCompositionChecks'],'composition.log')
run([a.java_home/'bin/java.exe','-Xmx512m','-ea','-cp',runtime,'inlineverification.VerifyFtbImeTransforms',repo/'src/main/resources',out/'native'],'transform.log')
