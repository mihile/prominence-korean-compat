"""Generate item references from actual quest SNBT. No probability or per-item bonus list."""
from pathlib import Path
import argparse,json,re
p=argparse.ArgumentParser();p.add_argument('--quests',type=Path,required=True);p.add_argument('--output',type=Path,required=True);a=p.parse_args()
ids=set()
for f in a.quests.rglob('*.snbt'):
 ids.update(re.findall(r'(?:item|id):\s*"([a-z0-9_.-]+:[a-z0-9_./-]+)"',f.read_text('utf8')))
a.output.parent.mkdir(parents=True,exist_ok=True);a.output.write_text(json.dumps(sorted(ids),ensure_ascii=False,indent=2)+'\n','utf8')
print('Quest references:',len(ids))
