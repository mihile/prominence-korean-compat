"""Merge client updater into an existing reviewed full compatibility JAR; never install it."""
from pathlib import Path
import argparse,hashlib,io,json,os,subprocess,zipfile
ROOT=Path(__file__).resolve().parents[1]
def sha(b):return hashlib.sha256(b).hexdigest()
def enc(o):return (json.dumps(o,ensure_ascii=False,indent=2)+'\n').encode('utf8')
def packed(d):
 out=io.BytesIO()
 with zipfile.ZipFile(out,'w',zipfile.ZIP_DEFLATED,compresslevel=9) as z:
  for n,b in sorted(d.items()):
   info=zipfile.ZipInfo(n,(2026,1,1,0,0,0));info.compress_type=zipfile.ZIP_DEFLATED;info.external_attr=0o100644<<16;z.writestr(info,b)
 return out.getvalue()
def safe_arg(p):
 p=Path(p)
 if not p.is_absolute():p=p.resolve()
 if os.name=='nt':
  import ctypes;buf=ctypes.create_unicode_buffer(1024);target=str(p) if p.is_dir() else str(p.parent)
  if ctypes.windll.kernel32.GetShortPathNameW(target,buf,1024)>0:
   return (Path(buf.value) if p.is_dir() else Path(buf.value)/p.name).as_posix()
 return p.as_posix()
def main():
 a=argparse.ArgumentParser();a.add_argument('--base',type=Path,required=True);a.add_argument('--classpath',type=Path,required=True);a.add_argument('--java-home',type=Path,required=True);a.add_argument('--build',type=int,required=True);a.add_argument('--label',required=True);a.add_argument('--track',choices=['main','nightly'],default='main');a.add_argument('--output',type=Path,required=True);args=a.parse_args()
 work=args.output/'work';work.mkdir(parents=True,exist_ok=True);classes=work/'classes';classes.mkdir(exist_ok=True)
 cp=[safe_arg(args.base),*[safe_arg(x) for x in json.loads(args.classpath.read_text('utf8'))]]
 sources=sorted((ROOT/'src/main/java').rglob('*.java'))
 options=['--release','17','-encoding','UTF-8','-proc:none','-classpath',os.pathsep.join(cp),'-d',safe_arg(classes),*[safe_arg(s) for s in sources]]
 argfile=work/'compile.args';argfile.write_text('\n'.join('"'+x+'"' for x in options),encoding='utf8')
 run=subprocess.run([str(args.java_home/'bin/javac.exe' if os.name=='nt' else args.java_home/'bin/javac'),'-J-Dfile.encoding=UTF-8','@'+safe_arg(argfile)],capture_output=True,text=True,encoding='utf8',errors='replace');(work/'compile.log').write_text(run.stdout+run.stderr,encoding='utf8');assert run.returncode==0,run.stdout+run.stderr
 with zipfile.ZipFile(args.base) as z:
  assert len(z.namelist())==len(set(z.namelist()));old={n:z.read(n) for n in z.namelist()}
 meta=json.loads(old['fabric.mod.json']);assert meta['id']=='prominence_korean_compat' and meta['version']=='1.0.0'
 new=dict(old)
 # These explicitly owned modules may be rebuilt; all other compatibility classes stay exact.
 managed_prefixes=('local/prominence/autoupdate/','local/prominence/memory/','local/prominence/backpackinline/','local/prominence/weaponhud/','local/prominence/swingscope/','local/prominence/localization/','local/prominence/ftbime/','local/prominence/join/','local/prominence/work/','local/prominence/lootcredit/','local/prominence/rewards/',
  'local/prominence/backpackcompat/client/PortableReforgeButton.class')
 for n in list(new):
  if n.startswith((*managed_prefixes,'META-INF/prominence-korean-compat-source/auto-update/')):del new[n]
 added={p.relative_to(classes).as_posix():p.read_bytes() for p in classes.rglob('*.class')}
 # Compile against cumulative APIs, but only replace the explicitly managed trees.
 # Unrelated source snapshots may have different compiler output from the reviewed base.
 new.update({n:b for n,b in added.items() if n not in old or n.startswith(managed_prefixes)})
 # Preserve cumulative classes that have no source in the current managed tree.
 for name,value in old.items():
  if name.endswith('.class') and name not in new:new[name]=value
 pub=(ROOT/'public-key.der').read_bytes()
 prefix='local/prominence/autoupdate/'
 helper={n:b for n,b in added.items() if n in [prefix+x+'.class' for x in ['ApplyUpdate','SignedRelease','UpdateFiles']]}
 assert len(helper)==3
 helper['assets/prominence_korean_compat/update-public-key.der']=pub
 helper['META-INF/MANIFEST.MF']=b'Manifest-Version: 1.0\nMain-Class: local.prominence.autoupdate.ApplyUpdate\n\n'
 helper_jar=packed(helper);resource='assets/prominence_korean_compat/'
 new[resource+'update-helper.jar']=helper_jar;new[resource+'update-public-key.der']=pub
 new[resource+'update-build.properties']=f'build={args.build}\nlabel={args.label}\nchannel=client\npack=4.1.3\ntrack={args.track}\nhelperSha256={sha(helper_jar)}\n'.encode('utf8')
 entry='local.prominence.autoupdate.UpdateClient';client=meta.setdefault('entrypoints',{}).setdefault('client',[])
 if entry not in client:client.append(entry)
 if (ROOT/'src/main/java/local/prominence/weaponhud/WeaponAbilityClient.java').exists():
  entry='local.prominence.weaponhud.WeaponAbilityClient'
  if entry not in client:client.append(entry)
 if (ROOT/'src/main/java/local/prominence/backpackinline/InlineReforge.java').exists():
  entry='local.prominence.backpackinline.InlineReforge';main=meta.setdefault('entrypoints',{}).setdefault('main',[])
  if entry not in main:main.append(entry)
  entry='local.prominence.backpackinline.client.InlineReforgeClient'
  if entry not in client:client.append(entry)
 if (ROOT/'src/main/java/local/prominence/memory/WorldReferencesClient.java').exists():
  entry='local.prominence.memory.WorldReferencesClient'
  if entry not in client:client.append(entry)
 if (ROOT/'src/main/java/local/prominence/memory/TransientHoldersClient.java').exists():
  entry='local.prominence.memory.TransientHoldersClient'
  if entry not in client:client.append(entry)
 if (ROOT/'src/main/java/local/prominence/memory/BatchMemoryClient.java').exists():
  entry='local.prominence.memory.BatchMemoryClient'
  if entry not in client:client.append(entry)
 if (ROOT/'src/main/java/local/prominence/memory/WorldCacheCleanupClient.java').exists():
  entry='local.prominence.memory.WorldCacheCleanupClient'
  if entry not in client:client.append(entry)
 entry='local.prominence.localization.IntegratedKoreanPack'
 if entry not in client:client.append(entry)
 pack=ROOT/'translation/Prominence_II_Korean_Translate.zip'
 for name in [n for n in new if n.startswith('resourcepacks/integrated_korean/')]:del new[name]
 with zipfile.ZipFile(pack) as z:
  names=z.namelist();assert len(names)==len(set(names)) and 'pack.mcmeta' in names
  for info in z.infolist():
   if info.is_dir():continue
   name=info.filename
   assert not name.startswith(('/', '\\')) and '\\' not in name and all(x not in ('', '.', '..') for x in name.split('/'))
   target='resourcepacks/integrated_korean/'+name
   assert target not in new
   new[target]=z.read(info)
 meta.setdefault('contact',{}).update(homepage='https://github.com/mihile/prominence-korean-compat',sources='https://github.com/mihile/prominence-korean-compat',issues='https://github.com/mihile/prominence-korean-compat/issues')
 entry=meta.setdefault('entrypoints',{}).setdefault('client',[])
 if 'local.prominence.rewards.client.RewardClientInit' not in entry:entry.append('local.prominence.rewards.client.RewardClientInit')
 common=meta.setdefault('entrypoints',{}).setdefault('main',[])
 if 'local.prominence.rewards.RewardCommonInit' not in common:common.append('local.prominence.rewards.RewardCommonInit')
 meta.setdefault('custom',{})['prominenceAutoUpdate']={'build':args.build,'channel':'client','pack':'4.1.3'}
 for p in sorted((ROOT/'src/main/resources').rglob('*')):
  if p.is_file():
   name=p.relative_to(ROOT/'src/main/resources').as_posix();new[name]=p.read_bytes()
   if name.endswith('.mixins.json'):
    entry={'config':name,'environment':'client'} if name.endswith('.client.mixins.json') else name
    if entry not in meta.setdefault('mixins',[]):meta['mixins'].append(entry)
 new['fabric.mod.json']=enc(meta)
 ko={'title':'한국어 호환 패치 업데이트','apply':'업데이트','later':'나중에','github':'GitHub','quit':'게임 종료','retry':'다시 시도','keep_playing':'계속 플레이','restart_hint':'게임 종료 후 적용 · 다음 실행부터 사용','downloading':'다운로드·검증 중…','ready':'준비 완료 · 게임 종료 후 적용','failed':'준비 실패 · 기존 파일은 유지됩니다.','check_failed':'업데이트를 확인하지 못했어요. 나중에 다시 시도해 주세요.','latest':'현재 최신 빌드입니다.','auto_on':'접속 시 업데이트 자동 확인을 켰습니다.','auto_off':'업데이트 자동 확인을 껐습니다.'}
 en={'title':'Korean Compatibility Update','apply':'Update','later':'Later','github':'GitHub','quit':'Quit game','retry':'Try again','keep_playing':'Keep playing','restart_hint':'Applies after the game closes.','downloading':'Downloading and verifying…','ready':'Ready · applies after game exit.','failed':'Update failed. Current file kept.','check_failed':'Cannot check for updates. Please try again later.','latest':'You have the latest build.','auto_on':'Automatic update checks enabled.','auto_off':'Automatic update checks disabled.'}
 for locale,values in [('ko_kr',ko),('en_us',en)]:
  values.update({'track_main':'안정판 (main)','track_nightly':'시험판 (nightly)','choose_track':'업데이트 채널을 선택하세요.','return_main':'안정판으로 돌아가기','return_hint':'안정판 복귀 · 게임 종료 후 적용'} if locale=='ko_kr' else {'track_main':'Stable (main)','track_nightly':'Nightly','choose_track':'Select an update channel.','return_main':'Return to stable','return_hint':'Return to stable after game exit'})
  values.update({'track_main_selected':'✓ 안정판 (main)','track_nightly_selected':'✓ 시험판 (nightly)','channel_main':'안정판 ▾','channel_nightly':'시험판 ▾'} if locale=='ko_kr' else {'track_main_selected':'✓ Stable (main)','track_nightly_selected':'✓ Nightly','channel_main':'Stable ▾','channel_nightly':'Nightly ▾'})
  values['up_to_date']='최신 상태' if locale=='ko_kr' else 'Up to date'
  values.update({'menu':'패치 업데이트','checking':'업데이트를 확인하고 있어요…','check_again':'다시 확인','close':'닫기'} if locale=='ko_kr' else {'menu':'Patch updates','checking':'Checking for updates…','check_again':'Check again','close':'Close'})
  n=resource+'lang/'+locale+'.json';d=json.loads(new[n]);d.update({'prominence.update.'+k:v for k,v in values.items()});new[n]=enc(d)
 for p in sources:new['META-INF/prominence-korean-compat-source/auto-update/'+p.relative_to(ROOT/'src/main/java').as_posix()]=p.read_bytes()
 legacy=ROOT/'src/main/java/local/prominence/backpackcompat/client/PortableReforgeButton.java'
 if legacy.exists():new['META-INF/prominence-korean-compat-source/backpack-workstations/local/prominence/backpackcompat/client/PortableReforgeButton.java']=legacy.read_bytes()
 new['META-INF/notices/auto-update.txt']=b'Original signed client updater, MIT. Only this compatibility JAR is updated with approval, after game exit. No server, settings, other mods, resource packs or launcher hooks are updated. Signing key is not distributed.\n'
 preserve=[n for n in old if n.endswith('.class') and not n.startswith(managed_prefixes)]
 changed=[n for n in preserve if new.get(n)!=old[n]]
 assert not changed, 'Unexpected cumulative class changes: '+', '.join(changed)
 jar=packed(new);args.output.mkdir(parents=True,exist_ok=True);(args.output/'prominence-korean-compat-1.0.0.jar').write_bytes(jar)
 report={'build':args.build,'label':args.label,'sha256':sha(jar),'size':len(jar),'addedUpdaterClasses':len(added),'preservedExistingClasses':len(preserve),'helperSha256':sha(helper_jar),'baseSha256':sha(args.base.read_bytes()),'runtimeValidated':False}
 (args.output/'build-report.json').write_bytes(enc(report));print(json.dumps(report,indent=2))
if __name__=='__main__':main()
