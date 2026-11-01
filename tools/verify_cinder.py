"""Verify installed Prominent targets without starting Minecraft or a server."""
from pathlib import Path
import argparse, json, os, subprocess, ctypes
p=argparse.ArgumentParser();p.add_argument('--build-dir',type=Path,required=True);a=p.parse_args()
r=Path(__file__).resolve().parents[1];b=a.build_dir.resolve();w=b/'native-cinder';c=w/'classes';c.mkdir(parents=True,exist_ok=True)
cp=[str(b/'prominence-korean-compat-1.0.0.jar'),*json.loads((r.parent/'build/reviewed3011/validation-classpath.json').read_text('utf8')),*map(str,(r.parent/'build/cinder-inspect').glob('*.jar')),*json.loads((r.parent/'classpath.json').read_text('utf8'))]
root=r.parents[2];buf=ctypes.create_unicode_buffer(2048);ctypes.windll.kernel32.GetShortPathNameW(str(root),buf,2048)
def run(exe,args,name):
    args=[str(x).replace('\\','/').replace(root.as_posix(),Path(buf.value).as_posix()) for x in args]
    f=w/(name+'.args');f.write_text('\n'.join('"'+x+'"' for x in args),encoding='utf8')
    out=subprocess.run(['C:/Java/graalvm-25.4.4.1.1+1.1/bin/'+exe,'@'+str(f)],capture_output=True,text=True,encoding='utf8',errors='replace');(w/(name+'.log')).write_text(out.stdout+out.stderr,encoding='utf8');print(out.stdout);assert out.returncode==0,out.stderr
run('javac.exe',['--release','17','-encoding','UTF-8','-proc:none','-cp',os.pathsep.join(cp),'-d',c,r/'tests/native/batchverification/VerifyMixinTransforms.java',r/'tests/native/cinderverification/VerifyCinder.java'],'compile')
s=c/'META-INF/services';s.mkdir(parents=True,exist_ok=True)
for k,v in {'org.spongepowered.asm.service.IMixinService':'batchverification.VerifyMixinTransforms','org.spongepowered.asm.service.IMixinServiceBootstrap':'batchverification.VerifyMixinTransforms$Bootstrap','org.spongepowered.asm.service.IGlobalPropertyService':'batchverification.VerifyMixinTransforms$Props'}.items():(s/k).write_text(v+'\n',encoding='utf8')
run('java.exe',['-Xmx512m','-cp',os.pathsep.join([str(c),*cp]),'cinderverification.VerifyCinder',w],'native')
