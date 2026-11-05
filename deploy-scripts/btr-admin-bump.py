# -*- coding: utf-8 -*-
import os, glob

fe = r"D:\Users\a3614\Documents\默认项目\frontend"
n = 0
for p in glob.glob(os.path.join(fe, "admin", "*.html")):
    with open(p, "r", encoding="utf-8") as f:
        c = f.read()
    c2 = c.replace("admin.js?v=20261105d", "admin.js?v=20261105e")
    if c2 != c:
        with open(p, "w", encoding="utf-8") as f:
            f.write(c2)
        n += 1
print("updated html:", n)
