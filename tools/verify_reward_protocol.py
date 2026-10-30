"""Cross-read real legacy/current reward wire implementations in separate short JVMs."""
from pathlib import Path
import argparse,json,os,subprocess
from build import safe_arg
p=argparse.ArgumentParser()
for n in ['baseline','jar','classpath','java-home','output']:p.add_argument('--'+n,type=Path,required=True)
a=p.parse_args();r=Path(__file__).resolve().parents[1];w=a.output.resolve();w.mkdir(parents=True,exist_ok=True)
c=w/'classes';c.mkdir(exist_ok=True);deps=json.loads(a.classpath.read_text('utf8'))
def run(java,args,name):
 af=w/(name+'.args');af.write_text('\n'.join('"'+str(x).replace('\\','/')+'"' for x in args),'utf8')
 q=subprocess.run([str(a.java_home/'bin'/java),'@'+safe_arg(af)],cwd=w,capture_output=True,text=True,encoding='utf8',errors='replace')
 (w/(name+'.log')).write_text(q.stdout+q.stderr,'utf8');assert q.returncode==0,q.stdout+q.stderr
 if q.stdout:print(q.stdout.strip())
cp=lambda jar:os.pathsep.join(map(safe_arg,[c,jar,*deps]))
run('javac.exe',['--release','17','-encoding','UTF-8','-proc:none','-cp',cp(a.baseline),'-d',safe_arg(c),safe_arg(r/'tests/RewardProtocolFixture.java')],'compile')
for mode,jar in [('emit-old',a.baseline),('emit-new',a.jar),('read-old',a.baseline)]:run('java.exe',['-cp',cp(jar),'local.prominence.rewards.RewardProtocolFixture',mode,safe_arg(w)],mode)
(w/'report.json').write_text(json.dumps({'status':'passed','oldV2ReadByNew':True,'newSafeV2ReadByOld':True,'v3RefusedByOld':True,'gameStarted':False},indent=2)+'\n','utf8')
