# -*- coding: utf-8 -*-
"""登录拿 token 实测 /admin/dashboard"""
import json, paramiko

KEY = "2eac3f0b-9463-45bc-bb93-9303e9cc3c8a"
CODE = "3V9L"

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

def run(c):
    i, o, e = ssh.exec_command(c, timeout=30)
    return o.read().decode("utf-8", "replace").strip()

body = json.dumps({"account": "a36143614u@163.com", "password": "Btr163#2026", "captchaKey": KEY, "captchaCode": CODE})
print("== 登录 ==")
out = run(f"curl -s -m 10 -X POST http://127.0.0.1:8082/api/v1/auth/login -H 'Content-Type: application/json' -d '{body}'")
print(out[:400])
try:
    d = json.loads(out)
    tok = d.get("data", {}).get("token") or d.get("data", {}).get("accessToken")
    print("token len:", len(tok) if tok else 0)
    if tok:
        print("== /admin/dashboard ==")
        print(run(f"curl -s -m 10 'http://127.0.0.1:8082/api/v1/admin/dashboard' -H 'Authorization: Bearer {tok}'")[:400])
        print("== /admin/users 抽查 ==")
        print(run(f"curl -s -m 10 -o /dev/null -w 'HTTP %{{http_code}}' 'http://127.0.0.1:8082/api/v1/admin/users?page=1&size=1' -H 'Authorization: Bearer {tok}'"))
except Exception as e:
    print("parse err:", e)
ssh.close()
print()
print("DONE")
