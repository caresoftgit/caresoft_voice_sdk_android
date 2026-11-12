#!/usr/bin/env python3
"""Verify the delivered archive before installing its local Maven repository."""
from pathlib import Path, PurePosixPath
import hashlib
import json
import shutil
import tempfile
import zipfile

ROOT = Path(__file__).resolve().parents[1]


def install(root=ROOT):
    packages = root / 'packages'
    release = json.loads((packages / 'release.json').read_text())
    name = release['binary']['file']
    if Path(name).name != name:
        raise ValueError('Invalid archive filename')
    archive = packages / name
    if hashlib.sha256(archive.read_bytes()).hexdigest() != release['binary']['sha256']:
        raise ValueError('SDK checksum mismatch; download the original package again')
    with tempfile.TemporaryDirectory(prefix='caresoft-setup-', dir=packages) as temporary:
        staging = Path(temporary)
        with zipfile.ZipFile(archive) as bundle:
            for entry in bundle.infolist():
                path = PurePosixPath(entry.filename)
                if (not path.parts or path.is_absolute() or '..' in path.parts
                        or path.parts[0] not in {'maven', 'THIRD_PARTY_NOTICES.md', 'licenses'}
                        or '\\' in entry.filename or (entry.external_attr >> 16) & 0o170000 == 0o120000):
                    raise ValueError('Unsafe SDK archive entry')
            bundle.extractall(staging)
        for artifact in ['voice-android', 'voice-android-ui']:
            base = staging / 'maven/vn/caresoft' / artifact / release['version']
            if not (base / f'{artifact}-{release["version"]}.aar').is_file():
                raise ValueError('Incomplete SDK package')
        target = packages / 'maven'
        if target.is_symlink():
            raise ValueError('Refusing to replace a symbolic link')
        if target.exists():
            shutil.rmtree(target)
        shutil.move(str(staging / 'maven'), target)
    print(f'Caresoft Voice Android {release["version"]}: verified and installed')


if __name__ == '__main__':
    install()
