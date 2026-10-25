"""Run after verify_inline_native.py. Native CPU/data fixture, never starts a player/world/game."""
from pathlib import Path
import argparse,json,os,subprocess
p=argparse.ArgumentParser()
for n in ['build-dir','classpath','java-home','libraries']:p.add_argument('--'+n,type=Path,required=True)
a=p.parse_args();repo=Path(__file__).resolve().parents[1];out=a.build_dir/'native-inline';classes=out/'classes';classes.mkdir(parents=True,exist_ok=True)
deps=json.loads(a.classpath.read_text('utf8'));extra=[s for s in deps if '2.3.13+' in s]
extra+=list(map(str,(a.libraries/'com/google/guava/failureaccess').glob('*/*.jar')))+list(map(str,(a.libraries/'io/netty').glob('*/4.1.82.Final/*.jar')))
cp=os.pathsep.join([str(classes),str(out/'verification/data-classes'),str(a.build_dir/'prominence-korean-compat-1.0.0.jar'),*extra,*deps])
def run(args,log):
 r=subprocess.run(list(map(str,args)),capture_output=True,text=True,encoding='utf8',errors='replace',cwd=out);(out/log).write_text(r.stdout+r.stderr,encoding='utf8');assert r.returncode==0,r.stdout+r.stderr;print(r.stdout)
params=['--release','17','-encoding','UTF-8','-proc:none','-cp',cp,'-d',str(classes.resolve()),str(repo/'tests/native/inlineverification/NativeInventoryProbe.java')]
# Paths are absolute because the fixture's data-only logs stay inside its staging directory.
params=[s if not (s==str(repo/'tests/native/inlineverification/NativeInventoryProbe.java')) else str((repo/'tests/native/inlineverification/NativeInventoryProbe.java').resolve()) for s in params]
cp=os.pathsep.join(str(Path(s).resolve()) for s in cp.split(os.pathsep));params[params.index('-cp')+1]=cp
af=out/'inventory-compile.args';af.write_text('\n'.join('"'+s.replace('\\','/')+'"' for s in params),encoding='utf8')
run([a.java_home/'bin/javac.exe','@'+str(af.resolve())],'inventory-compile.log')
run([a.java_home/'bin/java.exe','-Xmx512m','-ea','-cp',cp,'NativeInventoryProbe'],'inventory-probe.log')
