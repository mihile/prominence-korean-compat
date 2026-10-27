from pathlib import Path
import argparse,json,os,subprocess,zipfile
ROOT=Path(__file__).resolve().parents[1]
def main():
 a=argparse.ArgumentParser();a.add_argument('--build-dir',type=Path,required=True);a.add_argument('--classpath',type=Path,required=True);a.add_argument('--java-home',type=Path,required=True);a.add_argument('--fancymenu',type=Path);a.add_argument('--mods',type=Path);args=a.parse_args()
 exe='.exe' if os.name=='nt' else '';java=args.java_home/('bin/java'+exe);javac=args.java_home/('bin/javac'+exe)
 classes=args.build_dir/'work/classes';tests=args.build_dir/'test-classes';tests.mkdir(exist_ok=True)
 cp=os.pathsep.join([str(classes),str(args.build_dir/'prominence-korean-compat-1.0.0.jar'),*json.loads(args.classpath.read_text('utf8'))])
 params=['--release','17','-encoding','UTF-8','-proc:none','-cp',cp,'-d',str(tests),*[str(p) for p in sorted((ROOT/'tests').glob('*.java'))]]
 argfile=args.build_dir/'test-compile.args';argfile.write_text('\n'.join('"'+s.replace('\\','/')+'"' for s in params),encoding='utf8');subprocess.run([str(javac),'-J-Dfile.encoding=UTF-8','@'+str(argfile)],check=True)
 with zipfile.ZipFile(args.build_dir/'prominence-korean-compat-1.0.0.jar') as z:(args.build_dir/'test-helper.jar').write_bytes(z.read('assets/prominence_korean_compat/update-helper.jar'))
 results=[]
 checks=[('UpdaterTests',[str(args.build_dir/'test-helper.jar')]),('VerifyBytecode',[str(classes)]),('ModalRenderChecks',[str(classes)]),('local.prominence.memory.ShaderParserCacheTests',[]),('local.prominence.memory.ShaderCacheGuardTests',[]),('local.prominence.memory.BackpackListenerTests',[]),('local.prominence.memory.WorldReferenceTests',[])]
 checks.append(('local.prominence.memory.BetterDaysGuardTests',[]))
 checks.append(('local.prominence.memory.TransientHolderTests',[]))
 checks.append(('local.prominence.memory.BatchMemoryTests',[]))
 checks.append(('local.prominence.memory.ShaderSourceCacheTests',[]))
 checks.append(('local.prominence.memory.StorageRecoveryTests',[]))
 checks.append(('local.prominence.memory.WorldCacheTests',[]))
 checks.append(('local.prominence.memory.AzureQueryPrunerTests',[]))
 checks.append(('local.prominence.work.AttributeCandidatesTests',[]))
 checks.append(('local.prominence.memory.ShaderHashScratchTests',[]))
 checks.append(('local.prominence.memory.XaeroBiomeTests',[]))
 checks.append(('local.prominence.weaponhud.WeaponUseFenceTests',[]))
 checks.append(('local.prominence.swingscope.SwingScopeTests',[]))
 checks.append(('local.prominence.localization.TranslationOverrideTests',[]))
 if args.mods:checks.append(('local.prominence.weaponhud.WeaponHudPinChecks',[str(args.mods)]))
 if args.fancymenu:checks.append(('FancyMenuIsolationChecks',[str(args.fancymenu)]))
 for entry,params in checks:
  p=subprocess.run([str(java),'-ea','-cp',str(tests)+os.pathsep+cp,entry,*params],capture_output=True,text=True,encoding='utf8',errors='replace');(args.build_dir/(entry+'.log')).write_text(p.stdout+p.stderr,encoding='utf8');assert p.returncode==0,p.stdout+p.stderr;print(p.stdout.strip());results.append(p.stdout.strip())
 (args.build_dir/'verification.json').write_text(json.dumps({'status':'passed','results':results,'runtimeValidated':False,'gameStarted':False,'serverStarted':False},ensure_ascii=False,indent=2),encoding='utf8')
if __name__=='__main__':main()
