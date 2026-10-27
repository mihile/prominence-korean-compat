from pathlib import Path
import argparse,json,subprocess,os
p=argparse.ArgumentParser();p.add_argument('--build-dir',type=Path,required=True);p.add_argument('--classpath',type=Path,required=True);p.add_argument('--java-home',type=Path,required=True);a=p.parse_args()
r=Path(__file__).resolve().parents[1];w=a.build_dir/'native-azure-queries';c=w/'classes';c.mkdir(parents=True,exist_ok=True)
cp=os.pathsep.join([str((a.build_dir/'prominence-korean-compat-1.0.0.jar').resolve()),*json.loads(a.classpath.read_text('utf8'))]);j=a.java_home/'bin'
def run(cmd,name):
 q=subprocess.run(list(map(str,cmd)),capture_output=True,text=True,encoding='utf8',errors='replace');(w/name).write_text(q.stdout+q.stderr,'utf8');assert q.returncode==0,q.stdout+q.stderr;print(q.stdout)
run([j/'javac.exe','--release','17','-encoding','UTF-8','-proc:none','-cp',cp,'-d',c,r/'tests/AzureQueryPrunerTests.java'],'compile.log')
run([j/'java.exe','-Xmx256m','-ea','-Djava.awt.headless=true','-cp',str(c)+os.pathsep+cp,'local.prominence.memory.AzureQueryPrunerTests'],'lifetime.log')
