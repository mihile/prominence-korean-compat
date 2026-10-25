"""Data-only native registry and Tierify tooltip probe, no game/window/world/GL."""
from pathlib import Path
import argparse,json,os,subprocess
ROOT=Path(__file__).resolve().parents[1];p=argparse.ArgumentParser()
for n in ['build-dir','classpath','java-home']:p.add_argument('--'+n,type=Path,required=True)
a=p.parse_args();out=a.build_dir.resolve()/'native-tooltip';classes=out/'classes';classes.mkdir(parents=True,exist_ok=True)
cp=os.pathsep.join([str(classes),str((a.build_dir/'prominence-korean-compat-1.0.0.jar').resolve()),*json.loads(a.classpath.read_text('utf-8'))])
params=['--release','17','-encoding','UTF-8','-proc:none','-cp',cp,'-d',str(classes),str(ROOT/'tests/native/inlinetooltip/InlineTooltipProbe.java')];af=out/'compile.args';af.write_text('\n'.join('"'+s.replace('\\','/')+'"' for s in params),encoding='utf-8')
subprocess.run([str(a.java_home/'bin/javac.exe'),'@'+str(af)],check=True)
r=subprocess.run([str(a.java_home/'bin/java.exe'),'-Xmx512m','-ea','-cp',cp,'local.prominence.backpackinline.client.InlineTooltipProbe'],cwd=out,capture_output=True,text=True,encoding='utf-8',errors='replace');(out/'result.log').write_text(r.stdout+r.stderr,encoding='utf-8');assert r.returncode==0,r.stdout+r.stderr;print(r.stdout[-3000:])
