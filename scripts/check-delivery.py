#!/usr/bin/env python3
"""Check customer package boundaries, hashes, local documentation and nested archives."""
from pathlib import Path, PurePosixPath
import hashlib
import io
import json
import re
import zipfile

ROOT = Path(__file__).resolve().parents[1]
EXCLUDED = {'.DS_Store', '.git', '.gradle', '.kotlin', '.idea', '.work', 'build', '__pycache__'}


def inspect_bytes(name, data, forbidden_values=(), depth=0):
    path = PurePosixPath(name)
    if (path.name in {'google-services.json', 'local.properties', '.DS_Store'}
            or path.suffix in {'.p8', '.p12', '.jks', '.keystore', '.pem'}
            or '.local.' in path.name or '-sources' in path.name):
        raise ValueError('Private/source artifact in customer delivery: ' + name)
    if path.suffix in {'.kt', '.java', '.swift', '.c', '.cpp', '.h', '.m'}:
        # Only host demo/consumer examples are delivered as editable source.
        logical = re.split(r'\.(?:zip|apk|aar|jar)/', name)[-1]
        if not logical.startswith(('demo/src/', 'consumer-java/src/', 'integration/examples/')):
            raise ValueError('SDK implementation source in customer delivery: ' + name)
    if path.suffix == '.module':
        metadata = json.loads(data)
        for variant in metadata.get('variants', []):
            attrs = variant.get('attributes', {})
            if (attrs.get('org.gradle.category') == 'documentation'
                    or attrs.get('org.gradle.docstype') == 'sources'
                    or any('-sources' in f.get('url', '') for f in variant.get('files', []))):
                raise ValueError('Source variant in binary metadata: ' + name)
    for value in forbidden_values:
        if value and (value.encode() in data or value.encode('utf-16le') in data):
            raise ValueError('Private environment value in ' + name)
    if re.search(rb'-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----', data):
        raise ValueError('Private key in ' + name)
    if data.startswith(b'PK\x03\x04'):
        if depth > 8:
            raise ValueError('Archive nesting limit')
        with zipfile.ZipFile(io.BytesIO(data)) as archive:
            if archive.testzip() is not None:
                raise ValueError('Corrupt archive: ' + name)
            for entry in archive.infolist():
                if not entry.is_dir():
                    inspect_bytes(name + '/' + entry.filename, archive.read(entry), forbidden_values, depth + 1)


def check(root=ROOT, forbidden_values=()):
    for part in ['sdk', 'ui', 'distribution', 'deliverables']:
        if (root / part).exists():
            raise ValueError('Internal module/directory in customer repository: ' + part)
    for path in root.rglob('*'):
        parts = path.relative_to(root).parts
        if not path.is_file() or EXCLUDED.intersection(parts) or parts[:2] == ('packages', 'maven'):
            continue
        inspect_bytes(str(path.relative_to(root)), path.read_bytes(), forbidden_values)
    release = json.loads((root / 'packages/release.json').read_text())
    for key in ['binary', 'demo']:
        entry = release[key]
        artifact = root / 'packages' / entry['file']
        if hashlib.sha256(artifact.read_bytes()).hexdigest() != entry['sha256']:
            raise ValueError('Checksum mismatch: ' + entry['file'])
    checksums = root / 'packages/SHA256SUMS'
    if checksums.exists():
        for line in checksums.read_text().splitlines():
            digest, filename = line.split('  ', 1)
            if Path(filename).name != filename:
                raise ValueError('Invalid checksum filename')
            if hashlib.sha256((root / 'packages' / filename).read_bytes()).hexdigest() != digest:
                raise ValueError('Checksum mismatch: ' + filename)
    for path in [root / 'README.md', *list((root / 'docs').glob('*.md')), root / 'demo/README.md']:
        for link in re.findall(r'\]\(([^)]+)\)', path.read_text()):
            if link.startswith(('http:', 'https:', '#', 'mailto:')):
                continue
            if not (path.parent / link.split('#')[0]).exists():
                raise ValueError('Broken documentation link: ' + str(path) + ': ' + link)
    html = (root / 'docs/caresoft-voice-android.html').read_text()
    ids = set(re.findall(r'\bid="([^"]+)"', html))
    if any(a not in ids for a in re.findall(r'href="#([^"]+)"', html)):
        raise ValueError('Broken HTML anchor')
    print('Customer boundaries, nested archives, checksums and documentation: PASS')


if __name__ == '__main__':
    check()
