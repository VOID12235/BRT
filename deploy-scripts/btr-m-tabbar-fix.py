# -*- coding: utf-8 -*-
"""统一修复手机端 15 页底部导航图标（赛事 tab 图标当前损坏）"""
import os, re, glob

DIR = r"D:\Users\a3614\Documents\默认项目\frontend\m"
TABBAR = """  <nav class="m-tabbar">
    <a data-tab="home" href="/m/index.html"><svg viewBox="0 0 24 24"><path d="M4 10.5 12 3.5l8 7V20a1 1 0 0 1-1 1h-5v-6h-4v6H5a1 1 0 0 1-1-1z"/></svg>首页</a>
    <a data-tab="contests" href="/m/contests.html"><svg viewBox="0 0 24 24"><path d="M8 4h8v6a4 4 0 0 1-8 0z"/><path d="M8 6H4.5v1.5a3.5 3.5 0 0 0 3.5 3.5M16 6h3.5v1.5a3.5 3.5 0 0 1-3.5 3.5M12 14v3M8.5 21h7M10 17h4"/></svg>赛事</a>
    <a data-tab="rank" href="/m/rank.html"><svg viewBox="0 0 24 24"><path d="M5 20h14M7 20v-6h3v6M11 20V8h3v12M15 20V5h3v15"/></svg>榜单</a>
    <a data-tab="mine" href="/m/center.html"><svg viewBox="0 0 24 24"><circle cx="12" cy="8" r="4"/><path d="M5 20c0-3.5 3.1-5.5 7-5.5s7 2 7 5.5"/></svg>我的</a>
  </nav>"""

pattern = re.compile(r'<nav class="m-tabbar">.*?</nav>', re.S)
count = 0
for f in glob.glob(os.path.join(DIR, "*.html")):
    with open(f, "r", encoding="utf-8") as fp:
        content = fp.read()
    new, n = pattern.subn(TABBAR, content)
    if n:
        with open(f, "w", encoding="utf-8") as fp:
            fp.write(new)
        count += 1
        print("FIX", os.path.basename(f))
print("DONE", count, "pages")
