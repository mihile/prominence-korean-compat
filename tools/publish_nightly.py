from pathlib import Path
import argparse,hashlib,json,subprocess,zipfile
ROOT=Path(__file__).resolve().parents[1]; REPO='mihile/prominence-korean-compat'; JAR='prominence-korean-compat-1.0.0.jar'
def run(*args): subprocess.run([str(x) for x in args],check=True,cwd=ROOT)
def out(*args): return subprocess.check_output([str(x) for x in args],cwd=ROOT,text=True).strip()
def main():
 p=argparse.ArgumentParser();p.add_argument('--build-dir',type=Path,required=True);p.add_argument('--changes',type=Path,required=True);p.add_argument('--signing-key',type=Path,required=True);p.add_argument('--java-home',type=Path,required=True);p.add_argument('--notes-file',type=Path,required=True);a=p.parse_args()
 build=a.build_dir.resolve(); changes=a.changes.resolve(); notes=a.notes_file.resolve(); key=a.signing_key.resolve(); java=a.java_home.resolve();meta=json.loads(changes.read_text('utf8')); tag=meta['tag']; assert tag.startswith('nightly-') and tag[8:].replace('.','').isdigit()
 assert out('git','branch','--show-current')=='nightly' and not out('git','status','--porcelain')
 assert out('git','rev-parse','HEAD')==out('git','rev-parse','origin/nightly'),'Push reviewed source before publishing'
 with zipfile.ZipFile(build/JAR) as z: assert b'track=nightly\n' in z.read('assets/prominence_korean_compat/update-build.properties')
 run('python',ROOT/'tools/publish.py','--build-dir',build,'--changes',changes,'--signing-key',key,'--java-home',java,'--notes-file',notes,'--prepare-only')
 head=out('git','rev-parse','HEAD'); run('gh','release','create',tag,'--repo',REPO,'--target',head,'--prerelease','--title',meta['title'],'--notes-file',notes,build/'release'/JAR,build/'release/update.properties',build/'release/SHA256SUMS.txt')
 # Feed is a signed pointer to the immutable tagged asset, committed only after upload.
 feed=ROOT/'feed/update.properties';feed.parent.mkdir(exist_ok=True);feed.write_bytes((build/'release/update.properties').read_bytes())
 run('git','add','feed/update.properties');run('git','commit','-m',f'Publish {tag} feed');run('git','push','origin','nightly')
 print('Nightly published:',tag)
if __name__=='__main__':main()
