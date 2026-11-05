# -*- coding: utf-8 -*-
"""公网复现：连续密码错误 -> 锁定 -> 看锁定后响应体"""
import json, urllib.request, urllib.error

URL = "http://103.236.92.193:20757/api/v1/auth/login"
HEAD = {"Content-Type": "application/json"}

def login(pwd, captcha="XXXX", key="none"):
    body = json.dumps({"account": "a36143614u@163.com", "password": pwd, "captchaKey": key, "captchaCode": captcha}).encode()
    req = urllib.request.Request(URL, data=body, headers=HEAD, method="POST")
    try:
        with urllib.request.urlopen(req, timeout=15) as r:
            return r.status, r.read().decode("utf-8", "replace")
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode("utf-8", "replace")

for i in range(1, 8):
    st, txt = login("WRONGPWD%d" % i)
    print(f"第{i}次: HTTP {st} | {txt[:90]}")
