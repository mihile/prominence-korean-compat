"""Add only the reviewed client reach module and build/track metadata cumulatively."""
from pathlib import Path
import argparse, hashlib, importlib.util, json, os, subprocess, zipfile
ROOT = Path(__file__).resolve().parents[1]


def enc(value):
    return (json.dumps(value, ensure_ascii=False, indent=2) + '\n').encode('utf8')


def main():
    parser = argparse.ArgumentParser()
    for name in ['base', 'classpath', 'java-home', 'output']:
        parser.add_argument('--' + name, type=Path, required=True)
    parser.add_argument('--build', type=int, required=True)
    parser.add_argument('--label', required=True)
    args = parser.parse_args()
    out = args.output.resolve()
    classes = out / 'work/classes'
    module_classes = out / 'work/reach-classes'
    classes.mkdir(parents=True, exist_ok=True)
    module_classes.mkdir(parents=True, exist_ok=True)
    sources = sorted((ROOT / 'src/main/java/local/prominence/combatreach').rglob('*.java'))
    cp = os.pathsep.join([str(args.base.resolve()), *json.loads(args.classpath.read_text('utf8'))])
    options = ['--release', '17', '-encoding', 'UTF-8', '-proc:none', '-cp', cp, '-d', str(module_classes), *map(str, sources)]
    file = out / 'compile-reach.args'
    file.write_text('\n'.join('"' + s.replace('\\', '/') + '"' for s in options), 'utf8')
    result = subprocess.run([str(args.java_home / 'bin/javac.exe'), '-J-Xmx256m', '-J-Dfile.encoding=UTF-8', '@' + str(file)],
                            capture_output=True, text=True, encoding='utf8', errors='replace')
    (out / 'compile-reach.log').write_text(result.stdout + result.stderr, 'utf8')
    assert result.returncode == 0, result.stdout + result.stderr
    with zipfile.ZipFile(args.base) as jar:
        names = jar.namelist()
        assert len(names) == len(set(names))
        old = {n: jar.read(n) for n in names}
    meta = json.loads(old['fabric.mod.json'])
    assert meta['id'] == 'prominence_korean_compat' and meta['version'] == '1.0.0'
    assert args.build > meta['custom']['prominenceAutoUpdate']['build']
    added = {p.relative_to(module_classes).as_posix(): p.read_bytes() for p in module_classes.rglob('*.class')}
    assert len(added) == 3 and all(n.startswith('local/prominence/combatreach/') for n in added)
    for name in ['prominence-combat-reach.client.mixins.json', 'prominence-combat-reach-pins.json']:
        added[name] = (ROOT / 'src/main/resources' / name).read_bytes()
    for p in sources:
        added['META-INF/prominence-korean-compat-source/auto-update/' + p.relative_to(ROOT / 'src/main/java').as_posix()] = p.read_bytes()
    added['META-INF/notices/combat-reach.txt'] = b'Original guarded Better Combat / Reach Entity Attributes client bridge. MIT. No original mod classes included.\n'
    assert not set(added) & set(old), 'Use a new module only on a base that lacks it'
    new = {**old, **added}
    meta['mixins'].append({'config': 'prominence-combat-reach.client.mixins.json', 'environment': 'client'})
    meta['custom']['prominenceAutoUpdate']['build'] = args.build
    new['fabric.mod.json'] = enc(meta)
    prop = 'assets/prominence_korean_compat/update-build.properties'
    lines = old[prop].decode('utf8').splitlines()
    values = {'build': str(args.build), 'label': args.label, 'track': 'nightly'}
    for key in values:
        assert sum(s.startswith(key + '=') for s in lines) == 1
    updated_lines = []
    for line in lines:
        key = line.split('=', 1)[0]
        updated_lines.append(key + '=' + values[key] if key in values else line)
    new[prop] = ('\n'.join(updated_lines) + '\n').encode('utf8')
    allowed = {'fabric.mod.json', prop}
    assert all(new[n] == value for n, value in old.items() if n not in allowed)
    # Provide the complete cumulative classes to the existing ASM/cumulative checks.
    for name, data in new.items():
        if name.endswith('.class'):
            dest = classes / name
            dest.parent.mkdir(parents=True, exist_ok=True)
            dest.write_bytes(data)
    spec = importlib.util.spec_from_file_location('build_common', ROOT / 'tools/build.py')
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    raw = module.packed(new)
    (out / 'prominence-korean-compat-1.0.0.jar').write_bytes(raw)
    report = dict(build=args.build, label=args.label, track='nightly', size=len(raw), sha256=hashlib.sha256(raw).hexdigest(),
                  baseSha256=hashlib.sha256(args.base.read_bytes()).hexdigest(), addedReachClasses=3,
                  preservedExistingClasses=sum(n.endswith('.class') for n in old),
                  helperSha256=hashlib.sha256(old['assets/prominence_korean_compat/update-helper.jar']).hexdigest(),
                  updaterAndAllPreviousClassesPreserved=True, runtimeValidated=False)
    (out / 'build-report.json').write_bytes(enc(report))
    print(json.dumps(report, indent=2))


if __name__ == '__main__':
    main()
