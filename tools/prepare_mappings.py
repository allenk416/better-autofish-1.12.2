"""Extract Forge's obfuscation table without hard-coded machine paths."""
import argparse
import lzma
import zipfile
from pathlib import Path

parser = argparse.ArgumentParser()
parser.add_argument('--forge-jar', required=True, type=Path)
parser.add_argument('--output', required=True, type=Path)
args = parser.parse_args()

with zipfile.ZipFile(args.forge_jar) as archive:
    raw = lzma.decompress(archive.read('deobfuscation_data-1.12.2.lzma')).decode('utf-8')

classes, fields, methods = {}, {}, {}
for line in raw.splitlines():
    parts = line.split()
    if not parts:
        continue
    if parts[0] == 'CL:':
        classes[parts[1]] = parts[2]
    elif parts[0] == 'FD:':
        owner, name = parts[1].rsplit('/', 1)
        fields.setdefault(owner, []).append((name, parts[2].rsplit('/', 1)[1]))
    elif parts[0] == 'MD:':
        owner, name = parts[1].rsplit('/', 1)
        methods.setdefault(owner, []).append((name, parts[2], parts[3].rsplit('/', 1)[1]))

lines = []
for owner, name in classes.items():
    lines.append(f'{owner} {name}')
    lines.extend('\t' + ' '.join(item) for item in fields.get(owner, []))
    lines.extend('\t' + ' '.join(item) for item in methods.get(owner, []))

args.output.parent.mkdir(parents=True, exist_ok=True)
args.output.write_text('\n'.join(lines), encoding='utf-8')
print(f'Extracted mappings for {len(classes)} classes.')
