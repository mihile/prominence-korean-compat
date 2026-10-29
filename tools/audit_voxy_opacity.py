"""Audit native field references, including nested JARs, before shipping deferred opacity."""
import argparse, io, json, struct, zipfile
from pathlib import Path

OWNER='me/cortex/voxy/common/world/other/Mapper$StateEntry'
def refs(data):
    pos=8
    def u2():
        nonlocal pos
        v=struct.unpack_from('>H',data,pos)[0];pos+=2;return v
    size=u2();pool=[None]*size;i=1
    while i<size:
        tag=data[pos];pos+=1
        if tag==1:
            length=u2();pool[i]=(tag,data[pos:pos+length].decode('utf8','replace'));pos+=length
        elif tag in (7,8,16,19,20):pool[i]=(tag,u2())
        elif tag in (9,10,11,12,17,18):pool[i]=(tag,u2(),u2())
        elif tag in (3,4):pos+=4
        elif tag in (5,6):pos+=8;i+=1
        elif tag==15:pos+=3
        else:raise ValueError(tag)
        i+=1
    for entry in pool:
        if entry and entry[0]==9:
            owner=pool[pool[entry[1]][1]][1];nt=pool[entry[2]]
            if owner==OWNER and pool[nt[1]][1]=='opacity':yield pool[nt[2]][1]

def audit(source,label,found):
    with zipfile.ZipFile(source) as z:
        for name in z.namelist():
            if name.endswith('.class'):
                data=z.read(name)
                if OWNER.encode() in data and list(refs(data)):
                    found.append({'jar':label,'class':name})
                    if name not in (OWNER+'.class',OWNER.split('$')[0]+'.class'):
                        raise RuntimeError('Unwrapped opacity field consumer: '+label+'!'+name)
            elif name.endswith('.jar'):
                audit(io.BytesIO(z.read(name)),label+'!'+name,found)

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('mods',type=Path);p.add_argument('output',type=Path);a=p.parse_args()
    found=[];jars=list(a.mods.glob('*.jar'))
    for jar in jars:audit(jar,jar.name,found)
    if not found:raise RuntimeError('No Voxy field references found')
    a.output.parent.mkdir(parents=True,exist_ok=True)
    a.output.write_text(json.dumps({'status':'passed','topLevelJars':len(jars),'fieldReferences':found},indent=2)+'\n')
    print('PASS: scanned',len(jars),'JARs and nested dependencies; opacity references:',found)
