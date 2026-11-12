#!/usr/bin/env python3
from pathlib import Path
import markdown
root=Path(__file__).resolve().parents[1]
body=markdown.markdown((root/'docs/android-integration-guide.md').read_text(), extensions=['tables','fenced_code','toc'])
css='''body{font:16px/1.7 -apple-system,BlinkMacSystemFont,"Segoe UI",Arial,sans-serif;background:#f3f6f8;color:#123047;margin:0}main{max-width:1080px;margin:30px auto;padding:48px;background:white;border-radius:18px}h1,h2,h3{line-height:1.3;color:#087e75}h2{margin-top:44px;border-top:1px solid #dfe7eb;padding-top:22px}table{border-collapse:collapse;width:100%}td,th{border:1px solid #dfe7eb;padding:10px;text-align:left}pre{background:#123047;color:white;padding:20px;border-radius:12px;overflow:auto}code{font-family:monospace}a{color:#087e75}img{max-width:360px;width:100%;height:auto}footer{margin-top:40px;font-size:13px;color:#647787}@media(max-width:700px){main{padding:22px;margin:0;border-radius:0}h1{font-size:28px}}@media print{main{margin:0;padding:0;max-width:none}h2{break-before:page}pre{white-space:pre-wrap;background:#f3f6f8;color:#123047}tr{break-inside:avoid}}'''
html='<!doctype html><html lang="vi"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Caresoft Voice Android native</title><style>'+css+'</style></head><body><main>'+body+'<footer>Caresoft Voice Android · 1.0.0 · Native SDK</footer></main></body></html>'
(root/'docs/caresoft-voice-android.html').write_text(html)
print('Android integration HTML rendered')
