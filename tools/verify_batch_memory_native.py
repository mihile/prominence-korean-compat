from pathlib import Path
import subprocess,json,os,argparse
parser=argparse.ArgumentParser();parser.add_argument('--build-dir',type=Path,required=True);parser.add_argument('--classpath',type=Path,required=True);parser.add_argument('--java-home',type=Path,required=True);a=parser.parse_args()
REPO=Path(__file__).resolve().parents[1];W=a.build_dir/'native-memory-batch';W.mkdir(parents=True,exist_ok=True);J=a.java_home/'bin'
cp=[str((a.build_dir/'prominence-korean-compat-1.0.0.jar').resolve()),*json.loads((a.classpath).read_text('utf8'))]
C=W/'verification/classes';C.mkdir(parents=True,exist_ok=True)
def run(args,name):
 p=subprocess.run(list(map(str,args)),capture_output=True,text=True,encoding='utf8',errors='replace');(W/name).write_text(p.stdout+p.stderr,encoding='utf8');assert p.returncode==0,p.stdout+p.stderr;print(p.stdout[-2000:])
run([J/'javac.exe','--release','17','-encoding','UTF-8','-proc:none','-cp',os.pathsep.join(cp),'-d',C,REPO/'tests/native/batchverification/VerifyMixinTransforms.java'],'native-compile.log')
services=C/'META-INF/services';services.mkdir(parents=True,exist_ok=True)
for service,impl in {'org.spongepowered.asm.service.IMixinService':'batchverification.VerifyMixinTransforms','org.spongepowered.asm.service.IMixinServiceBootstrap':'batchverification.VerifyMixinTransforms$Bootstrap','org.spongepowered.asm.service.IGlobalPropertyService':'batchverification.VerifyMixinTransforms$Props'}.items():(services/service).write_text(impl+'\n',encoding='utf8')
run([J/'java.exe','-Xmx1g','-ea','-Djava.awt.headless=true','-Dmixin.service=batchverification.VerifyMixinTransforms','-Dmixin.bootstrapService=batchverification.VerifyMixinTransforms$Bootstrap','-cp',os.pathsep.join([str(C),*cp]),'batchverification.VerifyMixinTransforms',REPO/'src/main/resources/prominence-batch-memory.client.mixins.json',REPO/'tests/native/mmv-visual-targets.json',W/'verification',W/'native-mixin-report.json'],'native-transform.log')
