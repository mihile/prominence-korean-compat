"""Build conditional at-least-one probabilities; never convert an arbitrary mean to a chance."""
from pathlib import Path
import argparse, json, zipfile, math, hashlib

def distribution(value):
    if isinstance(value,(int,float)) and not isinstance(value,bool) and math.isfinite(value) and value>=0 and int(value)==value:return [(int(value),1.)]
    if not isinstance(value,dict):return None
    kind=value.get('type','').split(':')[-1]
    # Minecraft 1.20.1 also accepts the compact {min, max} uniform form.
    # Mine Cells uses this form for most of its entity drop counts.
    if not kind and set(value)=={'min','max'}:kind='uniform'
    if kind=='constant':return distribution(value.get('value'))
    if kind=='binomial':
        n,p=value.get('n'),value.get('p')
        if isinstance(n,(int,float)) and not isinstance(n,bool) and math.isfinite(n) and 0<=n<=64 and int(n)==n and isinstance(p,(int,float)) and not isinstance(p,bool) and math.isfinite(p) and 0<=p<=1:
            n=int(n)
            return [(k,math.comb(n,k)*p**k*(1-p)**(n-k)) for k in range(n+1)]
    if kind=='uniform':
        lo,hi=value.get('min'),value.get('max')
        if all(isinstance(x,(int,float)) and not isinstance(x,bool) and math.isfinite(x) and int(x)==x for x in (lo,hi)) and 0<=lo<=hi<=64:
            lo,hi=int(lo),int(hi)
            return [(k,1/(hi-lo+1)) for k in range(lo,hi+1)]
    return None

def kind(value):
    return value.removeprefix('minecraft:')

def positive_count(functions):
    """Probability of a nonempty stack; unknown context-dependent functions stay symbolic."""
    zero=0.
    safe={'set_damage','set_nbt','set_name','set_lore','set_enchantments','enchant_randomly','enchant_with_levels','set_potion','copy_nbt','copy_name','set_attributes','set_stew_effect','set_instrument','fill_player_head','exploration_map'}
    for f in functions:
        name=kind(f.get('function',''))
        if f.get('conditions'):return None
        if name=='set_count':
            d=distribution(f.get('count'))
            if d is None:return None
            z=sum(p for n,p in d if n==0)
            zero=zero*z if f.get('add',False) else z
        elif name=='looting_enchant':
            # A positive base count stays positive with a nonnegative bonus.
            # Zero base counts need the current Looting/float provider/rounding context.
            count=f.get('count');d=distribution(count)
            if zero or d is None or f.get('limit',0)<0:return None
        elif name not in safe:return None
    return 1-zero

def catalog(tables):
    out={};details={}
    for table,data in tables.items():
        rows={};bad=set();sources={}
        for pool in data.get('pools',[]):
            entries=pool.get('entries',[])
            def record(e):
                if kind(e.get('type',''))=='item' and isinstance(e.get('name'),str):
                    sources.setdefault(e['name'],[]).append({'rolls':pool.get('rolls',1),'bonus':pool.get('bonus_rolls',0),
                        'weight':e.get('weight',1),'total':sum(x.get('weight',1) for x in entries),'quality':e.get('quality',0),
                        'simple':all(kind(x.get('type','')) in ('item','empty') and not x.get('conditions') and not x.get('quality',0) for x in entries),
                        'conditions':pool.get('conditions',[]),'entryConditions':e.get('conditions',[]),
                        'functions':e.get('functions',[])+pool.get('functions',[])+data.get('functions',[])})
                for child in e.get('children',[]):record(child)
            for e in entries:record(e)
            dist=distribution(pool.get('rolls',1))
            if not dist or not entries or pool.get('bonus_rolls',0)!=0:continue
            if any(kind(e.get('type','')) not in ('item','empty') or e.get('quality',0)!=0 for e in entries):continue
            # Entry predicates affect selection normalization. A sole entry with one
            # draw is safe: EMI's live predicates are applied once by the tooltip.
            if any(e.get('conditions') for e in entries) and (len(entries)!=1 or dist!=[(1,1.)]):continue
            total=sum(e.get('weight',1) for e in entries)
            if total<=0:continue
            for e in entries:
                if kind(e['type'])!='item':continue
                item=e['name'];positive=positive_count(e.get('functions',[])+pool.get('functions',[])+data.get('functions',[]))
                if positive is None:
                    bad.add(item);continue
                q=e.get('weight',1)/total;mean=sum(n*w for n,w in dist)*q*100
                hit=sum((1-(1-q*positive)**n)*w for n,w in dist)*100
                gate=1.
                for condition in pool.get('conditions',[])+e.get('conditions',[]):
                    if kind(condition.get('condition',''))=='random_chance':gate*=condition.get('chance',0)
                    else:gate=None;break
                row=[mean,hit,gate]
                if item in rows:bad.add(item)
                rows[item]=row
        rows={k:v for k,v in rows.items() if k not in bad and len(sources.get(k,[]))==1}
        if rows:out[table]=rows
        if sources:details[table]=sources
    return out,details

def main():
    p=argparse.ArgumentParser();p.add_argument('--classpath',type=Path,required=True);p.add_argument('--output',type=Path,required=True);a=p.parse_args()
    tables={};adventure=None
    for path in json.loads(a.classpath.read_text('utf8')):
        file=Path(path)
        if not file.is_file() or not(file.parent.name=='mods' or file.name=='client-intermediary.jar'):continue
        with zipfile.ZipFile(file) as z:
            if file.name=='adventurez-1.4.20.jar':adventure=hashlib.sha256(z.read('net/adventurez/init/LootInit.class')).hexdigest()
            for name in z.namelist():
                if name.startswith('data/') and '/loot_tables/' in name and name.endswith('.json'):
                    ns,tail=name[5:].split('/loot_tables/',1)
                    try:tables[ns+':'+tail[:-5]]=json.loads(z.read(name))
                    except (ValueError,UnicodeError):pass
    out,details=catalog(tables)
    # AdventureZ's MODIFY callback injects a single Binomial draw, not a chance condition.
    # Reviewed against the installed class; reject upgrades until its callback is audited.
    if adventure:
        assert adventure=='874521a158f68c3df908f91f86d2935d170d46b3ae06573b4a0adec113514b1a','Re-audit AdventureZ MODIFY callback'
        with zipfile.ZipFile(next(Path(x) for x in json.loads(a.classpath.read_text('utf8')) if Path(x).name=='adventurez-1.4.20.jar')) as z:
            raw=z.read('net/adventurez/init/LootInit.class')
            assert b'GILDED_BLACKSTONE_SHARD' in raw
        for table,chance in [('minecraft:entities/piglin_brute',10),('minecraft:gameplay/piglin_bartering',1),*[(f'minecraft:chests/bastion_{s}',1) for s in ('bridge','hoglin_stable','other','treasure')]]:
            out.setdefault(table,{})['adventurez:gilded_blackstone_shard']=[chance,chance,1.]
    # Detailed models are only needed where exact rows are unavailable; avoid loading
    # thousands of redundant JSON objects into the client's retained display cache.
    details={t:{i:v for i,v in rows.items() if i not in out.get(t,{})} for t,rows in details.items()}
    details={t:r for t,r in details.items() if r}
    a.output.write_text(json.dumps({'tables':out,'details':details,'adventureZLootInitSha256':adventure},ensure_ascii=False,separators=(',',':'))+'\n','utf8')
    print('Conditional probability catalog:',len(out),'tables,',sum(map(len,out.values())),'items; AdventureZ callback',adventure)
if __name__=='__main__':main()
