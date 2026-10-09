"""Extract dependency classes for compilation, without mounting JARs as writable file systems."""
import argparse
import zipfile
from pathlib import Path, PurePosixPath

parser = argparse.ArgumentParser()
parser.add_argument('--output', required=True, type=Path)
parser.add_argument('--jars', nargs='+', required=True, type=Path)
args = parser.parse_args()
args.output.mkdir(parents=True, exist_ok=True)
for file in args.jars:
    with zipfile.ZipFile(file) as archive:
        for entry in archive.infolist():
            name = PurePosixPath(entry.filename)
            if name.is_absolute() or '..' in name.parts:
                raise RuntimeError('Unsafe dependency entry: ' + entry.filename)
            if not entry.is_dir() and entry.filename.endswith('.class'):
                target = args.output.joinpath(*name.parts)
                target.parent.mkdir(parents=True, exist_ok=True)
                target.write_bytes(archive.read(entry))
print('Extracted compiler dependency classes.')
