from pathlib import Path
import subprocess,json,os,argparse
parser=argparse.ArgumentParser();parser.add_argument('--build-dir',type=Path,required=True);parser.add_argument('--classpath',type=Path,required=True);parser.add_argument('--java-home',type=Path,required=True);a=parser.parse_args()
REPO=Path(__file__).resolve().parents[1];W=a.build_dir/'native-betterdays';W.mkdir(parents=True,exist_ok=True);J=a.java_home/'bin'
cp=[str((a.build_dir/'prominence-korean-compat-1.0.0.jar').resolve()),*json.loads((a.classpath).read_text('utf8'))]
C=W/'verification/classes';C.mkdir(parents=True,exist_ok=True)
def run(args,name):
    done=subprocess.run(list(map(str,args)),capture_output=True,text=True,encoding='utf8',errors='replace')
    (W/name).write_text(done.stdout+done.stderr,encoding='utf8');assert done.returncode==0,done.stdout+done.stderr
    print(done.stdout[-1500:])
run([J/'javac.exe','--release','17','-encoding','UTF-8','-proc:none','-cp',os.pathsep.join(cp),'-d',C,REPO/'tests/native/verification/VerifyMixinTransforms.java'],'native-compile.log')
services=C/'META-INF/services';services.mkdir(parents=True,exist_ok=True)
for service,implementation in {'org.spongepowered.asm.service.IMixinService':'verification.VerifyMixinTransforms','org.spongepowered.asm.service.IMixinServiceBootstrap':'verification.VerifyMixinTransforms$Bootstrap','org.spongepowered.asm.service.IGlobalPropertyService':'verification.VerifyMixinTransforms$Props'}.items():
    (services/service).write_text(implementation+'\n',encoding='utf8')
run([J/'java.exe','-Xmx768m','-ea','-Djava.awt.headless=true','-Dmixin.service=verification.VerifyMixinTransforms','-Dmixin.bootstrapService=verification.VerifyMixinTransforms$Bootstrap',
     '-cp',os.pathsep.join([str(C),*cp]),'verification.VerifyMixinTransforms',REPO/'src/main/resources/prominence-betterdays-memory.client.mixins.json',W/'native-mixin-report.json',W/'verification'],'native-transform.log')
