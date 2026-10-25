from pathlib import Path
import subprocess,json,os,argparse
p=argparse.ArgumentParser();p.add_argument('--build-dir',type=Path,required=True);p.add_argument('--classpath',type=Path,required=True);p.add_argument('--java-home',type=Path,required=True);a=p.parse_args()
REPO=Path(__file__).resolve().parents[1];W=a.build_dir/'native-storage';W.mkdir(parents=True,exist_ok=True);J=a.java_home/'bin'
cp=[str((a.build_dir/'prominence-korean-compat-1.0.0.jar').resolve()),*json.loads(a.classpath.read_text('utf8'))]
C=W/'classes';C.mkdir(parents=True,exist_ok=True)
def run(args,name):
 done=subprocess.run(list(map(str,args)),capture_output=True,text=True,encoding='utf8',errors='replace');(W/name).write_text(done.stdout+done.stderr,encoding='utf8');assert done.returncode==0,done.stdout+done.stderr;print(done.stdout[-1800:])
run([J/'javac.exe','--release','17','-encoding','UTF-8','-proc:none','-cp',os.pathsep.join(cp),'-d',C,REPO/'tests/native/batchverification/VerifyMixinTransforms.java',REPO/'tests/native/latencyverification/VerifyStorageTransforms.java'],'compile.log')
services=C/'META-INF/services';services.mkdir(parents=True,exist_ok=True)
for service,impl in {'org.spongepowered.asm.service.IMixinService':'batchverification.VerifyMixinTransforms','org.spongepowered.asm.service.IMixinServiceBootstrap':'batchverification.VerifyMixinTransforms$Bootstrap','org.spongepowered.asm.service.IGlobalPropertyService':'batchverification.VerifyMixinTransforms$Props'}.items():(services/service).write_text(impl+'\n',encoding='utf8')
run([J/'java.exe','-Xmx768m','-ea','-Djava.awt.headless=true','-Dmixin.service=batchverification.VerifyMixinTransforms','-Dmixin.bootstrapService=batchverification.VerifyMixinTransforms$Bootstrap','-cp',os.pathsep.join([str(C),*cp]),'latencyverification.VerifyStorageTransforms',REPO/'src/main/resources/prominence-voxy-storage.client.mixins.json',W/'verification'],'transform.log')
