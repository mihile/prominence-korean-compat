"""Update reviewed language resources without recompiling cumulative client code."""
from pathlib import Path
import argparse, hashlib, importlib.util, json, re, zipfile

ROOT = Path(__file__).resolve().parents[1]


def encoded(value):
    return (json.dumps(value, ensure_ascii=False, indent=2) + '\n').encode('utf8')


def main():
    parser = argparse.ArgumentParser()
    for name in ['base', 'output']:
        parser.add_argument('--' + name, type=Path, required=True)
    parser.add_argument('--build', type=int, required=True)
    parser.add_argument('--label', required=True)
    args = parser.parse_args()
    assert re.fullmatch(r'nightly-\d{8}\.\d+', args.label)
    with zipfile.ZipFile(args.base) as jar:
        old = {n: jar.read(n) for n in jar.namelist()}
        assert len(old) == len(jar.namelist())
    resource = 'assets/prominence_korean_compat/'
    identity = resource + 'update-build.properties'
    props = dict(line.split('=', 1) for line in old[identity].decode('utf8').splitlines() if '=' in line)
    assert props['track'] == 'nightly' and args.build > int(props['build'])
    meta = json.loads(old['fabric.mod.json'])
    assert meta['id'] == 'prominence_korean_compat' and meta['version'] == '1.0.0'
    meta['custom']['prominenceAutoUpdate']['build'] = args.build
    props.update(build=str(args.build), label=args.label)
    language = resource + 'translation_overrides/ko_kr.json'
    new = dict(old)
    new['fabric.mod.json'] = encoded(meta)
    new[identity] = ''.join(k + '=' + v + '\n' for k, v in props.items()).encode('utf8')
    new[language] = (ROOT / 'src/main/resources' / language).read_bytes()
    json.loads(new[language])
    changed = {n for n in old if old[n] != new[n]}
    assert changed == {'fabric.mod.json', identity, language}
    spec = importlib.util.spec_from_file_location('build_common', ROOT / 'tools/build.py')
    common = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(common)
    raw = common.packed(new)
    args.output.mkdir(parents=True, exist_ok=True)
    (args.output / 'prominence-korean-compat-1.0.0.jar').write_bytes(raw)
    classes = [n for n in old if n.endswith('.class')]
    assert all(new[n] == old[n] for n in classes)
    report = {'build': args.build, 'label': args.label, 'sha256': hashlib.sha256(raw).hexdigest(),
              'size': len(raw), 'baseSha256': hashlib.sha256(args.base.read_bytes()).hexdigest(),
              'preservedExistingClasses': len(classes), 'helperSha256': props['helperSha256'],
              'changedEntries': sorted(changed), 'allOtherEntriesPreserved': True,
              'runtimeValidated': False, 'gameStarted': False, 'serverStarted': False}
    (args.output / 'build-report.json').write_bytes(encoded(report))
    print(json.dumps(report, indent=2))


if __name__ == '__main__':
    main()
