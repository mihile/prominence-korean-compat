from pathlib import Path
import argparse,json,subprocess,os
p=argparse.ArgumentParser();p.add_argument('--build-dir',type=Path,required=True);p.add_argument('--classpath',type=Path,required=True);p.add_argument('--java-home',type=Path,required=True);a=p.parse_args()
r=Path(__file__).resolve().parents[1];w=(a.build_dir/'native-join').resolve();c=w/'classes';c.mkdir(parents=True,exist_ok=True)
cp=[str((a.build_dir/'prominence-korean-compat-1.0.0.jar').resolve()),*json.loads(a.classpath.read_text('utf8'))]
def run(cmd,name):
 argv=list(map(str,cmd)); af=w/(name+'.args')
 if os.name=='nt':
  import ctypes
  instance=r.parents[2];buf=ctypes.create_unicode_buffer(1024)
  if ctypes.windll.kernel32.GetShortPathNameW(str(instance),buf,1024):argv=[v.replace(str(instance),buf.value) for v in argv]
 af.write_text('\n'.join('"'+x.replace('\\','/')+'"' for x in argv[1:]),encoding='utf8')
 q=subprocess.run([argv[0],'@'+str(af)],capture_output=True,text=True,encoding='utf8',errors='replace',cwd=w);(w/name).write_text(q.stdout+q.stderr,'utf8');assert q.returncode==0,q.stdout+q.stderr;print(q.stdout[-2000:])
j=a.java_home/'bin'
run([j/'javac.exe','--release','17','-encoding','UTF-8','-proc:none','-cp',os.pathsep.join(cp),'-d',c,r/'tests/native/batchverification/VerifyMixinTransforms.java',r/'tests/native/joinverification/VerifyJoinTransforms.java',r/'tests/JoinWorkTests.java',r/'tests/JoinGuardTests.java',r/'tests/ResourceFilenameTests.java'],'compile.log')
s=c/'META-INF/services';s.mkdir(parents=True,exist_ok=True)
for key,value in {'org.spongepowered.asm.service.IMixinService':'batchverification.VerifyMixinTransforms','org.spongepowered.asm.service.IMixinServiceBootstrap':'batchverification.VerifyMixinTransforms$Bootstrap','org.spongepowered.asm.service.IGlobalPropertyService':'batchverification.VerifyMixinTransforms$Props'}.items():(s/key).write_text(value+'\n','utf8')
base=[j/'java.exe','-Xmx512m','-ea','-Djava.awt.headless=true','-cp',os.pathsep.join([str(c),*cp])]
run([*base,'local.prominence.join.JoinWorkTests'],'helpers.log')
run([*base,'local.prominence.join.ResourceFilenameTests'],'filenames.log')
run([*base,'local.prominence.join.JoinGuardTests',r/'src/main/resources/prominence-join-pins.json'],'guard.log')
run([*base,'-Dmixin.service=batchverification.VerifyMixinTransforms','-Dmixin.bootstrapService=batchverification.VerifyMixinTransforms$Bootstrap','joinverification.VerifyJoinTransforms',r/'src/main/resources/prominence-join.client.mixins.json',w/'verification'],'transform.log')
