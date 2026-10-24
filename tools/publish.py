"""Publish one reviewed client build with signed update metadata and release notes."""
from pathlib import Path
import argparse,hashlib,json,os,re,subprocess,zipfile
ROOT=Path(__file__).resolve().parents[1];REPO='mihile/prominence-korean-compat';FILE='prominence-korean-compat-1.0.0.jar'
def command(args,**kw):return subprocess.run(list(map(str,args)),check=True,**kw)
def main():
 a=argparse.ArgumentParser();a.add_argument('--build-dir',type=Path,required=True);a.add_argument('--changes',type=Path,required=True);a.add_argument('--signing-key',type=Path,required=True);a.add_argument('--java-home',type=Path,required=True);a.add_argument('--notes-file',type=Path,required=True);a.add_argument('--prepare-only',action='store_true');args=a.parse_args()
 assert not args.signing_key.resolve().is_relative_to(ROOT.resolve()),'Keep signing key outside repository'
 report=json.loads((args.build_dir/'build-report.json').read_text('utf8'));changes=json.loads(args.changes.read_text('utf8'));tag=changes['tag'];jar=args.build_dir/FILE
 assert len(changes['notes']) in range(1,41)
 raw=jar.read_bytes();assert hashlib.sha256(raw).hexdigest()==report['sha256'] and len(raw)==report['size']
 public=args.build_dir/'release';public.mkdir(exist_ok=True)
 import shutil;shutil.copy2(jar,public/FILE)
 props={'schema':'1','mod':'prominence_korean_compat','channel':'client','pack':'4.1.3','version':'1.0.0','filename':FILE,'build':str(report['build']),'label':report['label'],'title':changes['title'],'tag':tag,'url':f'https://github.com/{REPO}/releases/download/{tag}/{FILE}','size':str(len(raw)),'sha256':report['sha256']}
 props.update({'notes.'+str(i):v for i,v in enumerate(changes['notes'])})
 def escape(s):return str(s).replace('\\','\\\\').replace('\n','\\n').replace('\r','\\r')
 payload=args.build_dir/'manifest-payload.properties';payload.write_text(''.join(k+'='+escape(v)+'\n' for k,v in props.items()),encoding='utf8')
 tools=args.build_dir/'maintainer-tools';tools.mkdir(exist_ok=True);exe='.exe' if os.name=='nt' else '';java=args.java_home/('bin/java'+exe);javac=args.java_home/('bin/javac'+exe)
 command([javac,'--release','17','-encoding','UTF-8','-cp',jar,'-d',tools,ROOT/'tools/SignRelease.java',ROOT/'tools/VerifyRelease.java'])
 command([java,'-cp',tools,'SignRelease','sign',args.signing_key,payload,public/'update.properties'])
 command([java,'-cp',str(tools)+os.pathsep+str(jar),'VerifyRelease',public/'update.properties',jar])
 sums=''.join(hashlib.sha256((public/n).read_bytes()).hexdigest()+'  '+n+'\n' for n in [FILE,'update.properties']);(public/'SHA256SUMS.txt').write_text(sums,encoding='utf8')
 if args.prepare_only:return
 status=subprocess.check_output(['git','-C',str(ROOT),'status','--porcelain']).decode();assert not status.strip(),'Commit reviewed sources before publishing'
 match=re.fullmatch(r'build-(\d{8})\.(\d{1,2})',tag);assert match and report['build']==int(match[1]+match[2].zfill(2)), 'Tag/build mismatch'
 previous=json.loads(subprocess.check_output(['gh','api',f'repos/{REPO}/releases?per_page=100']))
 for release in previous:
  m=re.fullmatch(r'build-(\d{8})\.(\d{1,2})',release['tag_name'])
  if m and not release['draft']:assert report['build']>int(m[1]+m[2].zfill(2)),'Build must increase beyond published builds'
 commit=subprocess.check_output(['git','-C',str(ROOT),'rev-parse','HEAD']).decode().strip();assert len(commit)==40
 # The release target is always a full SHA, never a short revision or a branch guess.
 command(['gh','release','create',tag,'--repo',REPO,'--target',commit,'--latest','--title',changes['title'],'--notes-file',args.notes_file,public/FILE,public/'update.properties',public/'SHA256SUMS.txt'])
if __name__=='__main__':main()
