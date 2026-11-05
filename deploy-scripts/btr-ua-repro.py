# -*- coding: utf-8 -*-
import json, urllib.request, urllib.error

URL = "http://103.236.92.193:20757/api/v1/auth/login"
UA = "Mozilla/5.0 (Linux; Android 16; V2452A Build/BP2A.250605.031.A3_V000L1; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/156.0.8078.47 Mobile Safari/537.36 BTRApp/1.0"

def login(ua, referer):
    body = json.dumps({"account": "a36143614u@163.com", "password": "Btr163#2026", "captchaKey": "none", "captchaCode": "XXXX"}).encode()
    h = {"Content-Type": "application/json", "User-Agent": ua}
    if referer:
        h["Referer"] = referer
    req = urllib.request.Request(URL, data=body, headers=h, method="POST")
    try:
        with urllib.request.urlopen(req, timeout=15) as r:
            return r.status, r.read().decode("utf-8", "replace"), dict(r.headers)
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode("utf-8", "replace"), dict(e.headers)

for ua, ref in [
    (UA, "http://103.236.92.193:20757/m/login.html?redirect=%2Fm%2Fcenter.html"),
    (UA, None),
    ("Mozilla/5.0 (iPhone) BTRApp/1.0", "http://103.236.92.193:20757/m/login.html"),
    (UA, "http://103.236.92.193:20757/m/index.html"),
]:
    st, txt, h = login(ua, ref)
    ct = h.get("Content-Type", "")
    print(f"UA={'BTRApp' if 'BTRApp' in ua else 'other'} Referer={bool(ref)} -> HTTP {st} len={len(txt)} CT={ct}")
    print("   body:", txt[:120])
