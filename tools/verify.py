from pathlib import Path
import argparse,json,os,subprocess,zipfile
ROOT=Path(__file__).resolve().parents[1]
def safe_arg(p):
 p=Path(p)
 if not p.is_absolute():p=p.resolve()
 if os.name=='nt':
  import ctypes;buf=ctypes.create_unicode_buffer(1024);target=str(p) if p.is_dir() else str(p.parent)
  if ctypes.windll.kernel32.GetShortPathNameW(target,buf,1024)>0:
   return (Path(buf.value) if p.is_dir() else Path(buf.value)/p.name).as_posix()
 return p.as_posix()
def main():
 a=argparse.ArgumentParser();a.add_argument('--build-dir',type=Path,required=True);a.add_argument('--classpath',type=Path,required=True);a.add_argument('--java-home',type=Path,required=True);a.add_argument('--fancymenu',type=Path);a.add_argument('--mods',type=Path);args=a.parse_args()
 exe='.exe' if os.name=='nt' else '';java=args.java_home/('bin/java'+exe);javac=args.java_home/('bin/javac'+exe)
 classes=args.build_dir/'work/classes';tests=args.build_dir/'test-classes';tests.mkdir(exist_ok=True)
 cp=os.pathsep.join([safe_arg(args.build_dir/'prominence-korean-compat-1.0.0.jar'),safe_arg(classes),*[safe_arg(x) for x in json.loads(args.classpath.read_text('utf8'))]])
 params=['--release','17','-encoding','UTF-8','-proc:none','-cp',cp,'-d',safe_arg(tests),*[safe_arg(p) for p in sorted((ROOT/'tests').glob('*.java'))]]
 argfile=args.build_dir/'test-compile.args';argfile.write_text('\n'.join('"'+s+'"' for s in params),encoding='utf8');subprocess.run([str(javac),'-J-Dfile.encoding=UTF-8','@'+safe_arg(argfile)],check=True)
 with zipfile.ZipFile(args.build_dir/'prominence-korean-compat-1.0.0.jar') as z:(args.build_dir/'test-helper.jar').write_bytes(z.read('assets/prominence_korean_compat/update-helper.jar'))
 results=[]
 checks=[('UpdaterTests',[safe_arg(args.build_dir/'test-helper.jar')]),('VerifyBytecode',[safe_arg(classes)]),('ModalRenderChecks',[safe_arg(classes)]),('UpdaterChannelChecks',[safe_arg(classes)]),('local.prominence.memory.ShaderParserCacheTests',[]),('local.prominence.memory.ShaderCacheGuardTests',[]),('local.prominence.memory.BackpackListenerTests',[]),('local.prominence.memory.WorldReferenceTests',[])]
 checks.append(('local.prominence.work.VoxyScreenUvTests',[]))
 checks.append(('local.prominence.memory.BetterDaysGuardTests',[]))
 checks.append(('local.prominence.memory.TransientHolderTests',[]))
 checks.append(('local.prominence.memory.BatchMemoryTests',[]))
 checks.append(('local.prominence.memory.ShaderSourceCacheTests',[]))
 checks.append(('local.prominence.memory.StorageRecoveryTests',[]))
 checks.append(('local.prominence.memory.WorldCacheTests',[]))
 checks.append(('local.prominence.memory.AzureQueryPrunerTests',[]))
 checks.append(('local.prominence.work.AttributeCandidatesTests',[]))
 checks.append(('local.prominence.work.FtbQuestsFallbackStacksTests',[]))
 checks.append(('local.prominence.memory.ShaderHashScratchTests',[]))
 checks.append(('local.prominence.memory.XaeroBiomeTests',[]))
 checks.append(('local.prominence.weaponhud.WeaponUseFenceTests',[]))
 checks.append(('local.prominence.swingscope.SwingScopeTests',[]))
 checks.append(('local.prominence.localization.TranslationOverrideTests',[]))
 checks.append(('local.prominence.work.TrimPreviewAssetsTests',[]))
 checks.append(('local.prominence.work.DeferredOpacityTests',[]))
 checks.append(('local.prominence.work.StateOrderTests',[]))
 checks.append(('local.prominence.work.StonecuttingTransferTests',[]))
 checks.append(('local.prominence.work.LootUiTests',[]))
 checks.append(('local.prominence.rewards.RewardScalingTests',[]))
 checks.append(('local.prominence.rewards.client.RewardPollCacheTests',[]))
 checks.append(('MixinPackageBoundaryTests',[safe_arg(args.build_dir/'prominence-korean-compat-1.0.0.jar')]))
 import gzip
 live_fixture=args.build_dir/'reward-native-server.json';live_fixture.write_bytes(gzip.decompress((ROOT/'tests/fixtures/reward-native-server.json.gz').read_bytes()))
 checks.append(('local.prominence.rewards.RewardLiveDataTests',[safe_arg(live_fixture)]))
 checks.append(('local.prominence.work.CraftableRefreshTests',[]))
 checks.append(('SnapshotLeaseTests',[]))
 checks.append(('PerformanceRetirementChecks',[safe_arg(args.build_dir/'prominence-korean-compat-1.0.0.jar'),safe_arg(classes)]))
 checks.append(('local.prominence.work.ModelWorkSliceTests',[]))
 checks.append(('local.prominence.work.InitialWorkTests',[]))
 checks.append(('local.prominence.memory.ShaderTransformKeyTests',[safe_arg(ROOT/'src/main/resources/prominence-transform-key-pins.json')]))
 if args.mods:checks.append(('local.prominence.weaponhud.WeaponHudPinChecks',[safe_arg(args.mods)]))
 if args.fancymenu:checks.append(('FancyMenuIsolationChecks',[safe_arg(args.fancymenu)]))
 for entry,params in checks:
  run_args=['-ea','-cp',safe_arg(tests)+os.pathsep+cp,entry,*params]
  run_file=args.build_dir/'test-run.args'
  run_file.write_text('\n'.join('"'+s+'"' for s in run_args),encoding='utf8')
  p=subprocess.run([str(java),'@'+safe_arg(run_file)],capture_output=True,text=True,encoding='utf8',errors='replace',cwd=args.build_dir.resolve());(args.build_dir/(entry+'.log')).write_text(p.stdout+p.stderr,encoding='utf8');assert p.returncode==0,p.stdout+p.stderr;print(p.stdout.strip());results.append(p.stdout.strip())
 (args.build_dir/'verification.json').write_text(json.dumps({'status':'passed','results':results,'runtimeValidated':False,'gameStarted':False,'serverStarted':False},ensure_ascii=False,indent=2),encoding='utf8')
if __name__=='__main__':main()
