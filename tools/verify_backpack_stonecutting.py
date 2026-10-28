from pathlib import Path
import argparse,json,os,subprocess

p=argparse.ArgumentParser();p.add_argument('--build-dir',type=Path,required=True);p.add_argument('--classpath',type=Path,required=True);p.add_argument('--java-home',type=Path,required=True);a=p.parse_args()
r=Path(__file__).resolve().parents[1];w=a.build_dir/'native-stonecutting';c=w/'classes';c.mkdir(parents=True,exist_ok=True);j=a.java_home/'bin'
cp=[str((a.build_dir/'prominence-korean-compat-1.0.0.jar').resolve()),*json.loads(a.classpath.read_text('utf8'))]
# Native fingerprint/transform checks must inspect installed Voxy classes before any
# historical copies embedded in the cumulative compatibility artifact or fixture inputs.
native=[s for s in cp if Path(s).parent.name=='mods' and 'voxy' in Path(s).name.lower()]
cp=native+[s for s in cp if s not in native]
def run(cmd,name):
 argv=list(map(str,cmd)); af=w/(name+'.args')
 if os.name=='nt':
  import ctypes
  instance=Path.cwd().resolve(); buf=ctypes.create_unicode_buffer(1024)
  if ctypes.windll.kernel32.GetShortPathNameW(str(instance),buf,1024):argv=[v.replace(str(instance),buf.value) for v in argv]
 af.write_text('\n'.join('"'+x.replace('\\','/')+'"' for x in argv[1:]),encoding='utf8')
 cmd=[argv[0],'@'+str(af.resolve())]
 q=subprocess.run(list(map(str,cmd)),capture_output=True,text=True,encoding='utf8',errors='replace');(w/name).write_text(q.stdout+q.stderr,'utf8');assert q.returncode==0,q.stdout+q.stderr;print(q.stdout[-2000:])
run([j/'javac.exe','--release','17','-encoding','UTF-8','-proc:none','-cp',os.pathsep.join(cp),'-d',c,r/'tests/native/batchverification/VerifyMixinTransforms.java',r/'tests/native/workverification/VerifyBackpackAndRemap.java',r/'tests/StateOrderTests.java',r/'tests/StonecuttingTransferTests.java',r/'tests/JoinGuardTests.java'],'compile.log')
s=c/'META-INF/services';s.mkdir(parents=True,exist_ok=True)
for key,value in {'org.spongepowered.asm.service.IMixinService':'batchverification.VerifyMixinTransforms','org.spongepowered.asm.service.IMixinServiceBootstrap':'batchverification.VerifyMixinTransforms$Bootstrap','org.spongepowered.asm.service.IGlobalPropertyService':'batchverification.VerifyMixinTransforms$Props'}.items():(s/key).write_text(value+'\n','utf8')
classpath=os.pathsep.join([str(c.resolve()),*cp])
run([j/'java.exe','-cp',classpath,'local.prominence.work.StateOrderTests'],'state-order.log')
run([j/'java.exe','-cp',classpath,'local.prominence.work.StonecuttingTransferTests'],'stone-order.log')
run([j/'java.exe','-cp',classpath,'local.prominence.join.JoinGuardTests',r/'src/main/resources/prominence-emi-home-pins.json'],'guards.log')
run([j/'java.exe','-cp',classpath,'workverification.VerifyBackpackAndRemap',w/'cfg'],'transform.log')
(w/'report.json').write_text(json.dumps({'status':'passed','actualMixinTransformed':True,'previousSmithingCoexistenceVerified':True,'nativeStonecutterContractVerified':True,'nativeVersionAndHashGuardVerified':True,'gameStarted':False,'gameplayValidated':False},indent=2)+'\n','utf8')
