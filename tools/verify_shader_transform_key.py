"""Exercise original and Mixin-transformed installed Iris without starting a game or GL context."""
from pathlib import Path
import argparse,json,os,subprocess
from build import safe_arg

p=argparse.ArgumentParser()
for name in ['build-dir','classpath','java-home']:p.add_argument('--'+name,type=Path,required=True)
a=p.parse_args();root=Path(__file__).resolve().parents[1];work=(a.build_dir/'shader-key-verification').resolve();classes=work/'classes';classes.mkdir(parents=True,exist_ok=True)
cp=[safe_arg(a.build_dir/'prominence-korean-compat-1.0.0.jar'),*map(safe_arg,json.loads(a.classpath.read_text('utf8')))];java=a.java_home/'bin'
def run(exe,args,name):
    af=work/(name+'.args');af.write_text('\n'.join('"'+str(v).replace('\\','/')+'"' for v in args),'utf8')
    result=subprocess.run([str(java/(exe+'.exe')),'@'+safe_arg(af)],cwd=work,capture_output=True,text=True,encoding='utf8',errors='replace')
    (work/(name+'.log')).write_text(result.stdout+result.stderr,'utf8');assert result.returncode==0,result.stdout+result.stderr;print(result.stdout[-3500:])
sources=[root/'tests/native/batchverification/VerifyMixinTransforms.java',root/'tests/native/shaderverification/VerifyShaderTransforms.java',root/'tests/ShaderTransformKeyTests.java',root/'tests/RuntimeGsonAbiTests.java']
run('javac',['--release','17','-encoding','UTF-8','-proc:none','-cp',os.pathsep.join(cp),'-d',safe_arg(classes),*map(safe_arg,sources)],'compile')
services=classes/'META-INF/services';services.mkdir(parents=True,exist_ok=True)
service='batchverification.VerifyMixinTransforms'
for key,value in {'org.spongepowered.asm.service.IMixinService':service,'org.spongepowered.asm.service.IMixinServiceBootstrap':service+'$Bootstrap','org.spongepowered.asm.service.IGlobalPropertyService':service+'$Props'}.items():(services/key).write_text(value+'\n','utf8')
base=['-ea','-Xmx768m','-Djava.awt.headless=true','-cp',os.pathsep.join([safe_arg(classes),*cp])]
run('java',[*base,'RuntimeGsonAbiTests',safe_arg(a.build_dir/'prominence-korean-compat-1.0.0.jar')],'runtime-gson-links')
run('java',[*base,'local.prominence.memory.ShaderTransformKeyTests',safe_arg(root/'src/main/resources/prominence-transform-key-pins.json')],'parameters-guard')
run('java',[*base,'-Dmixin.service='+service,'-Dmixin.bootstrapService='+service+'$Bootstrap','shaderverification.VerifyShaderTransforms',safe_arg(root/'src/main/resources/prominence-transform-key.client.mixins.json'),safe_arg(work/'native')],'native')
