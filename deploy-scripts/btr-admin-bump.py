# -*- coding: utf-8 -*-
import os, glob

fe = r"D:\Users\a3614\Documents\榛樿椤圭洰\frontend"
n = 0
for p in glob.glob(os.path.join(fe, "admin", "*.html")):
    with open(p, "r", encoding="utf-8") as f:
        c = f.read()
    c2 = c.replace("admin.js?v=20261105e", "admin.js?v=20261105f")
    if c2 != c:
        with open(p, "w", encoding="utf-8") as f:
            f.write(c2)
        n += 1
print("updated html:", n)

