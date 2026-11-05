# -*- coding: utf-8 -*-
"""上传测试文件并验证各类型 inline 响应头"""
import base64, json, uuid, urllib.request

BASE = "http://103.236.92.193:20757/api/v1"

def upload_bytes(filename, ctype, raw, purpose="work"):
    boundary = uuid.uuid4().hex
    body = ('--%s\r\nContent-Disposition: form-data; name="file"; filename="%s"\r\n'
            'Content-Type: %s\r\n\r\n' % (boundary, filename, ctype)).encode() + raw + \
           ('\r\n--%s--\r\n' % boundary).encode()
    tok = open(r"D:\Users\a3614\Documents\默认项目\_probe\token.txt").read().strip()
    r = urllib.request.Request(BASE + "/files?purpose=" + purpose, data=body, method="POST",
                               headers={"Content-Type": "multipart/form-data; boundary=" + boundary,
                                        "Authorization": "Bearer " + tok})
    resp = urllib.request.urlopen(r, timeout=15)
    d = json.loads(resp.read())
    return d.get("data", {}).get("url", "")

def headers(url):
    r = urllib.request.Request("http://103.236.92.193:20757" + url)
    resp = urllib.request.urlopen(r, timeout=10)
    return resp.status, resp.headers.get("Content-Type"), resp.headers.get("Content-Disposition")

# 1x1 PNG
png = base64.b64decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==")
u1 = upload_bytes("测试图片.png", "image/png", png)
print("PNG 上传:", u1)
print("PNG 响应:", headers(u1))

# 文本
txt = "在线预览测试文本".encode("utf-8")
u3 = upload_bytes("测试文档.txt", "text/plain", txt)
print("TXT 上传:", u3)
print("TXT 响应:", headers(u3))
