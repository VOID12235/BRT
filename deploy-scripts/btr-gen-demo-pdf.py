# -*- coding: utf-8 -*-
"""生成 5 个最小合法 PDF（中文用 UTF-16BE hex）"""
import hashlib, os

OUT = os.path.join(r"D:\Users\a3614\Documents\默认项目", "demo_works")
os.makedirs(OUT, exist_ok=True)

WORKS = [
    ("6", "把夏天留在这里", "苏晓"),
    ("7", "潮汐来信", "陆娜"),
    ("8", "北纬三十一度", "周凯"),
    ("9", "慢下来 · 城市公共座椅计划", "米娅"),
    ("10", "纸上的风", "陆娜"),
]

def hex16(s):
    return "<FEFF" + s.encode("utf-16-be").hex().upper() + ">"

def build_pdf(title, author):
    t1 = hex16(title)
    t2 = hex16("BTR 赛事平台 - 作品展示文件")
    t3 = hex16("作者：" + author)
    t4 = hex16("本文件为平台演示作品文件，用于验证作品下载功能。")
    text = f"BT 60 780 Td /F1 24 Tf {t1} Tj 0 -34 Td /F1 14 Tf {t2} Tj 0 -26 Td {t3} Tj 0 -26 Td {t4} Tj ET"
    s1 = "1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj\n"
    s2 = "2 0 obj<</Type/Pages/Kids[3 0 R]/Count 1>>endobj\n"
    s3 = "3 0 obj<</Type/Page/Parent 2 0 R/MediaBox[0 0 595 842]/Contents 4 0 R/Resources<</Font<</F1 5 0 R>>>>>>endobj\n"
    s4 = f"4 0 obj<</Length {len(text)}>>stream\n{text}\nendstream\nendobj\n"
    s5 = "5 0 obj<</Type/Font/Subtype/Type1/BaseFont/Helvetica>>endobj\n"
    parts = [s1, s2, s3, s4, s5]
    out = "%PDF-1.4\n"
    offs = []
    p = len(out)
    for s in parts:
        offs.append(p)
        out += s
        p += len(s)
    xref = p
    out += f"xref\n0 6\n0000000000 65535 f \n"
    for o in offs:
        out += f"{o:010d} 00000 n \n"
    out += f"trailer<</Size 6/Root 1 0 R>>\nstartxref\n{xref}\n%%EOF\n"
    return out

for wid, title, author in WORKS:
    pdf = build_pdf(title, author)
    path = os.path.join(OUT, f"work{wid}.pdf")
    with open(path, "wb") as f:
        f.write(pdf.encode("latin-1"))
    md5 = hashlib.md5(pdf.encode("latin-1")).hexdigest()
    print(f"work{wid}: {path} {len(pdf)}B md5={md5}")
print("DONE")
