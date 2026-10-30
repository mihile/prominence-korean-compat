"""Verify installed native EMI method transformations and isolated data-only execution."""
from pathlib import Path
import argparse,json,os,subprocess
from build import safe_arg

p=argparse.ArgumentParser()
for name in ['build-dir','classpath','java-home']:p.add_argument('--'+name,type=Path,required=True)
a=p.parse_args();root=Path(__file__).resolve().parents[1];work=(a.build_dir/'emi-preparation-verification').resolve();classes=work/'classes';classes.mkdir(parents=True,exist_ok=True);fixture=work/'fixture';fixture.mkdir(exist_ok=True)
cp=[safe_arg(a.build_dir/'prominence-korean-compat-1.0.0.jar'),*map(safe_arg,json.loads(a.classpath.read_text('utf8')))];java=a.java_home/'bin'
def run(exe,args,name):
    af=work/(name+'.args');af.write_text('\n'.join('"'+str(v).replace('\\','/')+'"' for v in args),'utf8')
    result=subprocess.run([str(java/(exe+'.exe')),'@'+safe_arg(af)],cwd=work,capture_output=True,text=True,encoding='utf8',errors='replace')
    (work/(name+'.log')).write_text(result.stdout+result.stderr,'utf8');assert result.returncode==0,result.stdout+result.stderr;print(result.stdout[-3000:])
run('javac',['--release','17','-encoding','UTF-8','-proc:none','-cp',os.pathsep.join(cp),'-d',safe_arg(fixture),*map(safe_arg,(root/'tests/native/preparationfixture').rglob('*.java'))],'compile-fixture')
fixture_cp=os.pathsep.join([safe_arg(fixture),*cp])
run('javac',['--release','17','-encoding','UTF-8','-proc:none','-cp',fixture_cp,'-d',safe_arg(classes),safe_arg(root/'tests/native/batchverification/VerifyMixinTransforms.java'),safe_arg(root/'tests/native/workverification/VerifyEmiPreparation.java'),safe_arg(root/'tests/native/workverification/VerifySortedView.java'),safe_arg(root/'tests/native/workverification/AuditRecipeShapes.java')],'compile-native')
run('javac',['--release','17','-encoding','UTF-8','-proc:none','-cp',os.pathsep.join(cp),'-d',safe_arg(classes),safe_arg(root/'tests/JoinGuardTests.java'),safe_arg(root/'tests/RuntimeGsonAbiTests.java'),safe_arg(root/'tests/EmiPreparationPinTests.java')],'compile-guard')
services=classes/'META-INF/services';services.mkdir(parents=True,exist_ok=True);service='batchverification.VerifyMixinTransforms'
for key,value in {'org.spongepowered.asm.service.IMixinService':service,'org.spongepowered.asm.service.IMixinServiceBootstrap':service+'$Bootstrap','org.spongepowered.asm.service.IGlobalPropertyService':service+'$Props'}.items():(services/key).write_text(value+'\n','utf8')
native_cp=os.pathsep.join([safe_arg(classes),*cp]);base=['-ea','-Xmx768m','-Djava.awt.headless=true']
run('java',[*base,'-cp',native_cp,'local.prominence.join.JoinGuardTests',safe_arg(root/'src/main/resources/prominence-emi-preparation-pins.json')],'pins')
run('java',[*base,'-cp',native_cp,'EmiPreparationPinTests',safe_arg(root/'src/main/resources/prominence-emi-preparation-pins.json')],'actual-pins')
run('java',[*base,'-cp',native_cp,'RuntimeGsonAbiTests',safe_arg(a.build_dir/'prominence-korean-compat-1.0.0.jar')],'runtime-gson')
run('java',[*base,'-cp',os.pathsep.join([safe_arg(fixture),safe_arg(classes),*cp]),'-Dmixin.service='+service,'-Dmixin.bootstrapService='+service+'$Bootstrap','workverification.VerifyEmiPreparation',safe_arg(root/'src/main/resources/prominence-emi-preparation.client.mixins.json'),safe_arg(work/'native')],'native')

run('java',[*base,'-cp',os.pathsep.join([safe_arg(fixture),safe_arg(classes),*cp]),'-Dmixin.service='+service,'-Dmixin.bootstrapService='+service+'$Bootstrap','workverification.VerifySortedView',safe_arg(root/'src/main/resources/prominence-emi-preparation.client.mixins.json'),safe_arg(work/'sorted')],'sorted-view')

emi_jar=next(x for x in json.loads(a.classpath.read_text('utf8')) if Path(x).name=='emi-1.1.24+1.20.1+fabric.jar')
run('java',[*base,'-cp',native_cp,'workverification.AuditRecipeShapes',safe_arg(emi_jar),safe_arg(work/'native-shapes.json'),safe_arg(root/'src/main/resources/prominence-emi-fixed-recipes.json'),safe_arg(next(x for x in json.loads(a.classpath.read_text('utf8')) if Path(x).name=='Modern-Industrialization-1.8.6.jar'))],'native-shapes')
