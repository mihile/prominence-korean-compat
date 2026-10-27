from pathlib import Path
import argparse,json,os,subprocess

p=argparse.ArgumentParser();p.add_argument('--build-dir',type=Path,required=True);p.add_argument('--classpath',type=Path,required=True);p.add_argument('--java-home',type=Path,required=True);a=p.parse_args()
r=Path(__file__).resolve().parents[1];w=a.build_dir/'native-contention';c=w/'classes';c.mkdir(parents=True,exist_ok=True);j=a.java_home/'bin'
cp=[str((a.build_dir/'prominence-korean-compat-1.0.0.jar').resolve()),*json.loads(a.classpath.read_text('utf8'))]
def run(cmd,name):
 q=subprocess.run(list(map(str,cmd)),capture_output=True,text=True,encoding='utf8',errors='replace');(w/name).write_text(q.stdout+q.stderr,'utf8');assert q.returncode==0,q.stdout+q.stderr;print(q.stdout[-2000:])
run([j/'javac.exe','--release','17','-encoding','UTF-8','-proc:none','-cp',os.pathsep.join(cp),'-d',c,r/'tests/native/batchverification/VerifyMixinTransforms.java',r/'tests/native/workverification/VerifyJoinContention.java',r/'tests/native/workverification/RecipeNativeExecution.java',r/'tests/ModelWorkSliceTests.java',r/'tests/JoinGuardTests.java'],'compile.log')
s=c/'META-INF/services';s.mkdir(parents=True,exist_ok=True)
for key,value in {'org.spongepowered.asm.service.IMixinService':'batchverification.VerifyMixinTransforms','org.spongepowered.asm.service.IMixinServiceBootstrap':'batchverification.VerifyMixinTransforms$Bootstrap','org.spongepowered.asm.service.IGlobalPropertyService':'batchverification.VerifyMixinTransforms$Props'}.items():(s/key).write_text(value+'\n','utf8')
classpath=os.pathsep.join([str(c.resolve()),*cp])
run([j/'java.exe','-cp',classpath,'local.prominence.work.ModelWorkSliceTests'],'slice.log')
run([j/'java.exe','-cp',classpath,'local.prominence.join.JoinGuardTests',r/'src/main/resources/prominence-emi-home-pins.json'],'guards.log')
run([j/'java.exe','-cp',classpath,'workverification.VerifyJoinContention',w/'cfg'],'transform.log')
fc=w/'fixture-classes';fc.mkdir(exist_ok=True)
run([j/'javac.exe','--release','17','-encoding','UTF-8','-proc:none','-cp',os.pathsep.join(cp),'-d',fc,*sorted((r/'tests/native/recipefixture').rglob('*.java'))],'fixture-compile.log')
run([j/'java.exe','-Xmx512m','-cp',classpath,'workverification.RecipeNativeExecution',fc,w/'cfg/dev.emi.emi.registry.EmiRecipes$Manager.class'],'native-execution.log')
(w/'report.json').write_text(json.dumps({'status':'passed','actualNativeMixinTransformed':True,'gameStarted':False,'gameplayValidated':False},indent=2)+'\n','utf8')
