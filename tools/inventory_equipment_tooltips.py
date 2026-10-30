"""Inventory installed tooltip language and hardcoded tooltip classes; never mark a scan reviewed."""
from pathlib import Path
import argparse,json,re,zipfile,hashlib
p=argparse.ArgumentParser();p.add_argument('--mods',type=Path,required=True);p.add_argument('--output',type=Path,required=True);a=p.parse_args()
r=Path(__file__).resolve().parents[1];ko={}
with zipfile.ZipFile(r/'translation/Prominence_II_Korean_Translate.zip') as z:
 for n in z.namelist():
  if n.endswith('/lang/ko_kr.json'):
   try:ko.update(json.loads(z.read(n)))
   except (ValueError,UnicodeError):pass
ko.update(json.loads((r/'src/main/resources/assets/prominence_korean_compat/translation_overrides/ko_kr.json').read_text('utf8')))
prior={}
if a.output.exists():prior={x['source']+'|'+x['key']:x for x in json.loads(a.output.read_text('utf8'))['entries']}
entries=[];classes=[];mods=[]
for file in sorted(a.mods.glob('*.jar')):
 if 'prominence-korean-compat' in file.name:continue
 with zipfile.ZipFile(file) as z:
  if 'fabric.mod.json' not in z.namelist():continue
  meta=json.loads(z.read('fabric.mod.json'),strict=False);mod=meta.get('id',file.stem)
  mods.append({'id':mod,'file':file.name,'version':meta.get('version')})
  for n in z.namelist():
   if n.endswith('/lang/ko_kr.json'):
    try:
     for k,v in json.loads(z.read(n)).items():ko.setdefault(k,v)
    except (ValueError,UnicodeError):pass
  for n in z.namelist():
   if n.endswith('/lang/en_us.json'):
    try:lang=json.loads(z.read(n))
    except (ValueError,UnicodeError):continue
    for key,en in lang.items():
     if not isinstance(en,str) or not re.search(r'(tooltip|\.desc(?:ription)?(?:\d|\.|$)|\.ability\.)',key,re.I):continue
     if any(x in key.lower() for x in ['config','keybind','enchantment','enchant.','advancement']):continue
     translated=ko.get(key);row={'mod':mod,'source':file.name,'resource':n,'key':key,'english':en,'korean':translated,'status':'pending','mechanics':'unverified'}
     old=prior.get(file.name+'|'+key)
     if old and old.get('english')==en and old.get('korean')==translated:row.update({k:old[k] for k in ['status','mechanics','notes'] if k in old})
     row['screening']={'missingKorean':not isinstance(translated,str),'sameAsEnglish':translated==en}
     entries.append(row)
   elif n.endswith('.class') and any(s in n.lower() for s in ['item','weapon','armor','trinket','accessor','tooltip']):
    data=z.read(n)
    if b'method_7851' in data or b'appendTooltip' in data:classes.append({'mod':mod,'source':file.name,'class':n,'sha256':hashlib.sha256(data).hexdigest(),'status':'pending','reason':'Native tooltip method/reference; includes dynamically generated and literal text'})
result={'scope':'Conservative candidates; includes non-equipment descriptions until classified. Dynamic tooltip classes require separate manual review. Inventory is not completed review.','mods':mods,'entries':entries,'nativeTooltipClasses':classes}
a.output.parent.mkdir(parents=True,exist_ok=True);a.output.write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n','utf8')
print('Inventoried',len(entries),'language entries,',len(classes),'native tooltip classes across',len(mods),'mods; no automatic review approvals')
