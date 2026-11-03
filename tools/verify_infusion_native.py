"""Real Mixin transforms + exact raw class/version guard checks; no game/world/GL."""
from pathlib import Path
import argparse,json,os,subprocess,ctypes,zipfile,hashlib
p=argparse.ArgumentParser()
for n in ['build-dir','classpath','java-home']:p.add_argument('--'+n,type=Path,required=True)
p.add_argument('--jar',type=Path);p.add_argument('--side',choices=['CLIENT','SERVER'],default='CLIENT')
a=p.parse_args();repo=Path(__file__).resolve().parents[1];out=a.build_dir/'infusion-native';classes=out/'classes';classes.mkdir(parents=True,exist_ok=True)
cp=[str((a.jar or a.build_dir/'prominence-korean-compat-1.0.0.jar').resolve()),*json.loads(a.classpath.read_text('utf8'))]
def short(path):
 path=str(Path(path).resolve());buf=ctypes.create_unicode_buffer(32768)
 return buf.value if ctypes.windll.kernel32.GetShortPathNameW(path,buf,len(buf)) else path
def run(argv,name):
 result=subprocess.run(list(map(str,argv)),capture_output=True,text=True,encoding='utf8',errors='replace');(out/name).write_text(result.stdout+result.stderr,'utf8');assert result.returncode==0,result.stdout+result.stderr;print(result.stdout[-1800:])
args=['--release','17','-proc:none','-encoding','UTF-8','-cp',os.pathsep.join(cp),'-d',str(classes),str(repo/'tests/native/batchverification/VerifyMixinTransforms.java'),str(repo/'tests/native/infusionverification/VerifyInfusionTransforms.java')]
f=out/'compile.args';f.write_text('\n'.join('"'+s.replace('\\','/')+'"' for s in args),'utf8');run([a.java_home/'bin/javac.exe','@'+str(f)],'compile.log')
services=classes/'META-INF/services';services.mkdir(exist_ok=True,parents=True)
for name,impl in {'org.spongepowered.asm.service.IMixinService':'batchverification.VerifyMixinTransforms','org.spongepowered.asm.service.IMixinServiceBootstrap':'batchverification.VerifyMixinTransforms$Bootstrap','org.spongepowered.asm.service.IGlobalPropertyService':'batchverification.VerifyMixinTransforms$Props'}.items():(services/name).write_text(impl+'\n')
args=['-Xmx768m','-ea','-cp',os.pathsep.join(short(p) for p in [classes,*cp]),'infusionverification.VerifyInfusionTransforms',short(repo/'src/main/resources'),short(out)+'/verification',a.side]
f=out/'runtime.args';f.write_text('\n'.join('"'+s.replace('\\','/')+'"' for s in args),'utf8');run([a.java_home/'bin/java.exe','@'+str(f)],'transform.log')

pins=json.loads((repo/'src/main/resources/prominence-infusion-pins.json').read_text('utf8'))
checks=0
for mod,pin in pins.items():
 found=False
 for path in cp:
  try:
   with zipfile.ZipFile(path) as z:
    if 'fabric.mod.json' not in z.namelist():continue
    meta=json.loads(z.read('fabric.mod.json'),strict=False)
    if meta['id']!=mod:continue
    assert meta['version']==pin['version']
    for name,digest in pin['classes'].items():assert hashlib.sha256(z.read(name)).hexdigest()==digest,name;checks+=1
    found=True;break
  except (OSError,zipfile.BadZipFile):pass
 assert found,mod
print('Exact infusion native fingerprints verified:',checks,'side:',a.side)
