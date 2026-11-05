# -*- coding: utf-8 -*-
"""端到端：带 Origin 头拿验证码->识别->登录->调管理接口"""
import base64, json, re, urllib.request, urllib.error

BASE = "http://103.236.92.193:20757/api/v1"
ORIGIN = "http://103.236.92.193:20757"

def req(path, method="GET", body=None, headers=None):
    h = {"Origin": ORIGIN, "User-Agent": "Mozilla/5.0 BTRApp/1.0"}
    if headers:
        h.update(headers)
    data = json.dumps(body).encode() if body is not None else None
    r = urllib.request.Request(BASE + path, data=data, headers=h, method=method)
    try:
        with urllib.request.urlopen(r, timeout=15) as resp:
            return resp.status, resp.read()
    except urllib.error.HTTPError as e:
        return e.code, e.read()

# 1) 验证码
st, raw = req("/auth/captcha")
print("captcha:", st)
d = json.loads(raw.decode())
key = d.get("data", {}).get("key")
img = d.get("data", {}).get("image", "")
open(r"D:\Users\a3614\Documents\默认项目\_probe\cap_e2e.png", "wb").write(base64.b64decode(img))
print("key:", key, "img saved")
