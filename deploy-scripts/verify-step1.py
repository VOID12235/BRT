# -*- coding: utf-8 -*-
"""端到端验证：登录 + 配置赛题 + 验证字段"""
import urllib.request, json, base64, sys, io

BASE = "http://103.236.92.193:20757/api/v1"

def req(path, method="GET", body=None, token=None):
    url = BASE + path
    data = json.dumps(body).encode() if body is not None else None
    r = urllib.request.Request(url, data=data, method=method, headers={
        "User-Agent": "Mozilla/5.0", "Content-Type": "application/json",
        "Authorization": ("Bearer " + token) if token else "",
    })
    try:
        return json.load(urllib.request.urlopen(r, timeout=30))
    except urllib.error.HTTPError as e:
        return {"_http": e.code, "_body": e.read().decode("utf-8", "replace")}

# 1. 拿验证码
cap = req("/auth/captcha")
print("captcha enabled:", cap.get("data", {}).get("enabled"))
key = cap["data"]["key"]
img_b64 = cap["data"]["image"]
if img_b64.startswith("data:"):
    img_b64 = img_b64.split(",", 1)[1]
open(r"D:\Users\a3614\Documents\默认项目\.preview\captcha.png", "wb").write(base64.b64decode(img_b64))
print("captcha key:", key)
print("captcha img saved")
