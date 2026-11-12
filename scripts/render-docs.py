#!/usr/bin/env python3
"""Render the customer integration documents into one self-contained HTML file."""
from pathlib import Path
from urllib.parse import unquote
import html
import re
import markdown

ROOT = Path(__file__).resolve().parents[1]
DOCUMENTS = [
    ('android-integration-guide.md', 'guide', 'Tích hợp'),
    ('api.md', 'api', 'API Kotlin/Java'),
    ('backend-contract.md', 'backend', 'Backend và Firebase'),
    ('compatibility.md', 'compatibility', 'Yêu cầu Android'),
    ('security.md', 'security', 'Bảo vệ credential'),
    ('testing.md', 'testing', 'Nghiệm thu'),
    ('troubleshooting.md', 'troubleshooting', 'Xử lý lỗi'),
    ('integration-delivery.md', 'delivery', 'Gói cài đặt'),
]
SECTIONS = {name: section for name, section, _ in DOCUMENTS}
# Keep Vietnamese heading anchors consistent with the Markdown links in the repo.
def slugify(value, separator):
    value = value.lower()
    value = re.sub(r'[^\w\s-]', '', value, flags=re.UNICODE)
    return re.sub(r'[\s-]+', separator, value).strip(separator)


def render_document(name, section):
    body = markdown.markdown(
        (ROOT / 'docs' / name).read_text(),
        extensions=['tables', 'fenced_code', 'toc'],
        extension_configs={'toc': {'slugify': slugify}},
    )
    body = re.sub(r'\bid="([^"]+)"', lambda m: f'id="{section}-{m[1]}"', body)

    def link(match):
        target = html.unescape(match[1])
        path, _, anchor = target.partition('#')
        if not path:
            return f'href="#{section}-{anchor}"'
        if path in SECTIONS:
            destination = SECTIONS[path]
            if anchor:
                destination += '-' + unquote(anchor)
            return f'href="#{html.escape(destination, quote=True)}"'
        return match[0]

    body = re.sub(r'href="([^"]+)"', link, body)
    return f'<section id="{section}">{body}</section>'


css = '''body{font:16px/1.7 -apple-system,BlinkMacSystemFont,"Segoe UI",Arial,sans-serif;background:#f3f6f8;color:#123047;margin:0}main{max-width:1080px;margin:30px auto;padding:48px;background:white;border-radius:18px}nav{padding:18px;background:#edf7f5;border-radius:12px}nav a{display:inline-block;margin:4px 14px 4px 0}h1,h2,h3{line-height:1.3;color:#087e75}h2{margin-top:44px;border-top:1px solid #dfe7eb;padding-top:22px}section+section{margin-top:72px;border-top:4px solid #087e75;padding-top:24px}table{border-collapse:collapse;width:100%}td,th{border:1px solid #dfe7eb;padding:10px;text-align:left;overflow-wrap:anywhere}pre{background:#123047;color:white;padding:20px;border-radius:12px;overflow:auto}code{font-family:monospace}a{color:#087e75}img{max-width:360px;width:100%;height:auto}footer{margin-top:40px;font-size:13px;color:#647787}@media(max-width:700px){main{padding:22px;margin:0;border-radius:0}h1{font-size:28px}table{font-size:14px}td,th{padding:6px}}@media print{main{margin:0;padding:0;max-width:none}h2,section+section{break-before:page}pre{white-space:pre-wrap;background:#f3f6f8;color:#123047}tr{break-inside:avoid}}'''
nav = '<nav aria-label="Tài liệu">' + ''.join(
    f'<a href="#{section}">{label}</a>' for _, section, label in DOCUMENTS
) + '</nav>'
body = nav + ''.join(render_document(name, section) for name, section, _ in DOCUMENTS)
output = ('<!doctype html><html lang="vi"><head><meta charset="utf-8">'
          '<meta name="viewport" content="width=device-width,initial-scale=1">'
          '<title>Caresoft Voice Android 1.0.2 — Tài liệu tích hợp</title>'
          f'<style>{css}</style></head><body><main>{body}'
          '<footer>Caresoft Voice Android · 1.0.2</footer></main></body></html>')
ids = re.findall(r'\bid="([^"]+)"', output)
assert len(ids) == len(set(ids)), 'Duplicate HTML anchor'
for anchor in re.findall(r'href="#([^"]+)"', output):
    assert anchor in ids, f'Broken HTML anchor: {anchor}'
(ROOT / 'docs/caresoft-voice-android.html').write_text(output)
print(f'Android integration HTML rendered: {len(DOCUMENTS)} documents, local anchors verified')
