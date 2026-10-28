from pathlib import Path
import argparse,json,subprocess,os,zipfile
p=argparse.ArgumentParser();p.add_argument('--build-dir',type=Path,required=True);p.add_argument('--classpath',type=Path,required=True);p.add_argument('--java-home',type=Path,required=True);a=p.parse_args();repo=Path(__file__).resolve().parents[1];build=a.build_dir.resolve();classes=build/'pack-fixture';classes.mkdir(exist_ok=True);jar=build/'prominence-korean-compat-1.0.0.jar';cp=os.pathsep.join([str(jar),*json.loads(a.classpath.read_text('utf8'))]);j=str(a.java_home/'bin')+'/'
def run(args):
 p=subprocess.run(args,text=True,capture_output=True,encoding='utf8',errors='replace');print(p.stdout,p.stderr[-1500:]);assert p.returncode==0
run([j+'javac.exe','--release','17','-encoding','UTF-8','-proc:none','-cp',cp,'-d',str(classes),*[str(p) for p in (repo/'tests/native/koreanpackfixture').rglob('*.java')]])
run([j+'java.exe','-cp',str(classes)+os.pathsep+cp,'fixture.Run'])
with zipfile.ZipFile(repo/'translation/Prominence_II_Korean_Translate.zip') as source,zipfile.ZipFile(jar) as target:
 items=[n for n in source.namelist() if not n.endswith('/')];assert all(target.read('resourcepacks/integrated_korean/'+n)==source.read(n) for n in items)
 meta=json.loads(target.read('fabric.mod.json'));assert 'local.prominence.localization.IntegratedKoreanPack' in meta['entrypoints']['client'];rules=json.loads(target.read('assets/prominence_korean_compat/quest_reviewed_text.json'));assert len(rules)>=2670
 print('PASS: original resource files verified',len(items),'quest exact rules',len(rules),'entrypoint present')
