# -*- coding: utf-8 -*-
"""批量升级 m/*.html 与 rank.html 的版本号"""
import os, re

FE = r"D:\Users\a3614\Documents\默认项目\frontend"
mdir = os.path.join(FE, "m")

def repl(content):
    c = content
    c = c.replace("m.js?v=20261105b4", "m.js?v=20261105b5")
    c = c.replace("m.css?v=20261105m3", "m.css?v=20261105m3")
    c = c.replace("assets/js/rank.js?v=20261105e", "assets/js/rank.js?v=20261105f")
    return c

n = 0
for name in os.listdir(mdir):
    if not name.endswith(".html"):
        continue
    p = os.path.join(mdir, name)
    with open(p, "r", encoding="utf-8") as f:
        orig = f.read()
    new = repl(orig)
    if new != orig:
        with open(p, "w", encoding="utf-8") as f:
            f.write(new)
        n += 1
print("m/*.html 更新:", n)

p = os.path.join(FE, "rank.html")
with open(p, "r", encoding="utf-8") as f:
    orig = f.read()
new = repl(orig)
with open(p, "w", encoding="utf-8") as f:
    f.write(new)
print("rank.html 更新:", new != orig)

# 校验
import glob
for f in glob.glob(os.path.join(mdir, "*.html")):
    with open(f, "r", encoding="utf-8") as fh:
        c = fh.read()
    if "m.js?v=20261105b3" in c:
        print("!! 仍旧:", os.path.basename(f))
print("校验完成")
