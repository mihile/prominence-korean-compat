"""Verify arrival policy and actual installed Mixin transforms without starting Minecraft."""
from pathlib import Path
import argparse,json,os,subprocess
from build import safe_arg

p=argparse.ArgumentParser()
for name in ['build-dir','classpath','java-home']:p.add_argument('--'+name,type=Path,required=True)
p.add_argument('--server-service',type=Path);p.add_argument('--jar',type=Path);p.add_argument('--side',choices=['CLIENT','SERVER'],default='CLIENT')
a=p.parse_args();root=Path(__file__).resolve().parents[1];work=(a.build_dir/'arrival-verification').resolve();classes=work/'classes';classes.mkdir(parents=True,exist_ok=True)
jar=(a.jar or a.build_dir/'prominence-korean-compat-1.0.0.jar').resolve()
cp=[safe_arg(jar),*map(safe_arg,json.loads(a.classpath.read_text('utf8')))];java=a.java_home/'bin'
def run(exe,args,name):
    af=work/(name+'.args');af.write_text('\n'.join('"'+str(v).replace('\\','/')+'"' for v in args),'utf8')
    result=subprocess.run([str(java/(exe+'.exe')),'@'+safe_arg(af)],cwd=work,capture_output=True,text=True,encoding='utf8',errors='replace')
    (work/(name+'.log')).write_text(result.stdout+result.stderr,'utf8');assert result.returncode==0,result.stdout+result.stderr;print(result.stdout[-2000:])
service='verification.VerifyMixinTransforms' if a.side=='SERVER' else 'batchverification.VerifyMixinTransforms'
if a.side=='SERVER':assert a.server_service and a.server_service.is_file(),'Pass the existing dedicated-server offline service source'
sources=[a.server_service if a.side=='SERVER' else root/'tests/native/batchverification/VerifyMixinTransforms.java',root/'tests/native/arrivalverification/VerifyArrivalTransforms.java',root/'tests/ArrivalGuardTests.java',root/'tests/ArrivalProtectionTests.java']
if a.side=='SERVER':sources.append(root/'src/main/java/local/prominence/arrival/client/ArrivalClientGate.java')
run('javac',['--release','17','-encoding','UTF-8','-proc:none','-cp',os.pathsep.join(cp),'-d',safe_arg(classes),*map(safe_arg,sources)],'compile')
services=classes/'META-INF/services';services.mkdir(parents=True,exist_ok=True)
for key,value in {'org.spongepowered.asm.service.IMixinService':service,'org.spongepowered.asm.service.IMixinServiceBootstrap':service+'$Bootstrap','org.spongepowered.asm.service.IGlobalPropertyService':service+'$Props'}.items():(services/key).write_text(value+'\n','utf8')
base=['-ea','-Xmx512m','-Djava.awt.headless=true','-cp',os.pathsep.join([safe_arg(classes),*cp])]
run('java',[*base,'local.prominence.arrival.ArrivalProtectionTests'],'policy')
run('java',[*base,'local.prominence.arrival.ArrivalGuardTests',safe_arg(root/'src/main/resources/prominence-arrival-pins.json')],'guard')
run('java',[*base,'-Dmixin.service='+service,'-Dmixin.bootstrapService='+service+'$Bootstrap','arrivalverification.VerifyArrivalTransforms',safe_arg(root/'src/main/resources/prominence-arrival.mixins.json'),safe_arg(work/'transformed'),service],'transforms')
