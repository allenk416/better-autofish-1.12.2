"""Package all mod classes/resources from this source tree; never patch an older JAR."""
import argparse
import hashlib
import json
import re
import zipfile
from pathlib import Path

root = Path(__file__).resolve().parents[1]

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--classes-dir', required=True, type=Path)
    args = parser.parse_args()
    version = (root / 'VERSION').read_text(encoding='ascii').strip()
    if not re.fullmatch(r'1\.0\.[0-9]+', version):
        raise RuntimeError('VERSION must be an explicit 1.0.N version.')
    classes = sorted(args.classes_dir.rglob('*.class'))
    if not classes:
        raise RuntimeError('No compiled mod classes.')
    for path in classes:
        name = path.relative_to(args.classes_dir).as_posix()
        if not name.startswith('com/northwestwind/forgeautofish/'):
            raise RuntimeError('Unexpected non-mod class: ' + name)
        data = path.read_bytes()
        if data[:4] != b'\xca\xfe\xba\xbe' or int.from_bytes(data[6:8], 'big') != 52:
            raise RuntimeError('Not Java 8 bytecode: ' + name)
    required = ['AutoFish.class', 'handler/AutoFishHandler.class', 'handler/FishingPolicy.class',
                'handler/FishingSettingsScreen.class', 'keybind/KeyBinds.class',
                'proxy/ClientProxy.class', 'proxy/CommonProxy.class']
    for name in required:
        if not (args.classes_dir / 'com/northwestwind/forgeautofish' / name).is_file():
            raise RuntimeError('Missing compiled class: ' + name)
    resources = root / 'src/main/resources'
    metadata = json.loads((resources / 'mcmod.info').read_text(encoding='utf-8'))
    if metadata[0]['version'] != version or metadata[0]['modid'] != 'forgeautofish':
        raise RuntimeError('VERSION and mcmod.info must agree.')
    if 'updateUrl' in metadata[0] or 'updateJSON' in metadata[0]:
        raise RuntimeError('Update metadata must not be inherited from another project.')
    out = root / 'dist'
    out.mkdir(exist_ok=True)
    target = out / ('Better-AutoFish-1.12.2-' + version + '.jar')
    source_zip = out / ('Better-AutoFish-1.12.2-' + version + '-source.zip')
    if target.exists() or source_zip.exists():
        raise RuntimeError('Refusing to overwrite this version. Increase VERSION and mcmod.info together.')
    manifest = ('Manifest-Version: 1.0\r\nImplementation-Title: Better AutoFish 1.12.2\r\n'
                'Implementation-Version: ' + version + '\r\nImplementation-Vendor: allenk416\r\n\r\n')
    with target.open('xb') as stream, zipfile.ZipFile(stream, 'w', zipfile.ZIP_DEFLATED) as jar:
        jar.writestr('META-INF/MANIFEST.MF', manifest)
        for file in classes:
            jar.write(file, file.relative_to(args.classes_dir).as_posix())
        for file in sorted(resources.rglob('*')):
            if file.is_file():
                jar.write(file, file.relative_to(resources).as_posix())
        for name in ('LICENSE', 'NOTICE.md'):
            jar.write(root / name, name)
    with zipfile.ZipFile(target) as jar:
        if jar.testzip() is not None or len(jar.namelist()) != len(set(jar.namelist())):
            raise RuntimeError('JAR integrity validation failed.')
    source_files = []
    for folder in ('src', 'tools', 'tests', 'docs'):
        source_files.extend(p for p in (root / folder).rglob('*')
                            if p.is_file() and '__pycache__' not in p.parts and p.suffix != '.pyc')
    source_files.extend(root / name for name in ('.gitignore', 'VERSION', 'LICENSE', 'NOTICE.md',
                                                'README.md', 'README.zh-CN.md', 'BUILDING.md'))
    with source_zip.open('xb') as stream, zipfile.ZipFile(stream, 'w', zipfile.ZIP_DEFLATED) as archive:
        for file in sorted(source_files):
            archive.write(file, file.relative_to(root).as_posix())
    hashes = []
    for file in (target, source_zip):
        hashes.append(hashlib.sha256(file.read_bytes()).hexdigest() + '  ' + file.name)
    (out / ('SHA256SUMS-' + version + '.txt')).write_text('\n'.join(hashes) + '\n', encoding='ascii')
    print(json.dumps({'version': version, 'jar': target.name, 'source': source_zip.name,
                      'sha256': hashes}, indent=2))

if __name__ == '__main__':
    main()
