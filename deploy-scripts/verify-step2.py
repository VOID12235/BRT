# -*- coding: utf-8 -*-
"""登录 admin + 给赛事2 配赛题 + 验证详情"""
import urllib.request, json

BASE = "http://103.236.92.193:20757/api/v1"

def req(path, method="GET", body=None, token=None):
    data = json.dumps(body).encode() if body is not None else None
    r = urllib.request.Request(BASE + path, data=data, method=method, headers={
        "User-Agent": "Mozilla/5.0", "Content-Type": "application/json",
        "Authorization": ("Bearer " + token) if token else "",
    })
    try:
        return json.load(urllib.request.urlopen(r, timeout=30))
    except urllib.error.HTTPError as e:
        return {"_http": e.code, "_body": e.read().decode("utf-8", "replace")}

login = req("/auth/login", "POST", {
    "account": "a36143614u", "password": "Btr163#2026",
    "captchaKey": "08991fcd-04d7-47d0-bd51-1e611925a1ea", "captchaCode": "448R",
})
print("login:", login.get("code"), login.get("message"))
token = login.get("data", {}).get("token")
if not token:
    print("登录失败，终止"); raise SystemExit

# 给赛事2（打分中）配赛题附件（用平台已有文件 URL 测试）
put = req("/contests/2", "PUT", {
    "topicFileUrl": "/files/contest2/topic-photo.pdf",
    "topicFileName": "摄影大赛赛题说明.pdf",
}, token)
print("put contest2:", put.get("code"), put.get("message"))

# 详情验证（公开接口，无需 token）
d = req("/contests/2")
c = d.get("data", {}).get("contest", {})
print("detail c2:", c.get("title"), "| status:", c.get("status"))
print("topicFileUrl:", c.get("topicFileUrl"))
print("topicFileName:", c.get("topicFileName"))
