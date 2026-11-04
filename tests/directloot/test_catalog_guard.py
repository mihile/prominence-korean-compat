from pathlib import Path
import json,subprocess,sys,tempfile,zipfile
root=Path(__file__).resolve().parents[2]
with tempfile.TemporaryDirectory() as t:
 p=Path(t);mods=p/'mods';mods.mkdir();jar=mods/'fixture.jar'; cp=p/'classpath.json';out=p/'output.json'
 def table(name,fn):return (f'data/check/loot_tables/entities/{name}.json',json.dumps({'pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'minecraft:stone','functions':fn}]}]}))
 with zipfile.ZipFile(jar,'w') as z:
  for name,fn in [('safe',[{'function':'minecraft:set_count','count':{'min':1,'max':3}}]),('zero_alias',[{'function':'set_count','count':{'min':0,'max':2}}]),('zero_namespaced',[{'function':'minecraft:set_count','count':{'min':0,'max':2}}]),('explosion',[{'function':'minecraft:explosion_decay'}]),('unknown',[{'function':'foreign:unknown'}]),('name',[{'function':'minecraft:set_name','name':'demo'}])]:
   n,b=table(name,fn);z.writestr(n,b)
 cp.write_text(json.dumps([str(jar)]));subprocess.run([sys.executable,str(root/'tools/build_loot_chances.py'),'--classpath',str(cp),'--output',str(out)],check=True)
 d=json.loads(out.read_text())['tables'];assert set(d)=={'check:entities/safe','check:entities/name'},d
 print('PASS safe count and cosmetic functions; zero count, explosion decay and unknown functions rejected')
