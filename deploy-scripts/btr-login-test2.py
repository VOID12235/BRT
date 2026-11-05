# -*- coding: utf-8 -*-
"""最终验证：登录 + /admin/dashboard + /admin/users"""
import json, paramiko

KEY = "51c64dbc-8b5b-4acc-ae8c-ac485790d659"
CODE = "993W"

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

def run(c, timeout=30):
    i, o, e = ssh.exec_command(c, timeout=timeout)
    return o.read().decode("utf-8", "replace").strip()

body = json.dumps({"account": "a36143614u@163.com", "password": "Btr163#2026", "captchaKey": KEY, "captchaCode": CODE})
out = run(f"curl -s -m 10 -X POST http://127.0.0.1:8082/api/v1/auth/login -H 'Content-Type: application/json' -d '{body}'")
print("== 登录 ==")
print(out[:220])
d = json.loads(out)
tok = d.get("data", {}).get("accessToken")
print("token len:", len(tok) if tok else 0)
if tok:
    print("== /admin/dashboard ==")
    print(run(f"curl -s -m 10 'http://127.0.0.1:8082/api/v1/admin/dashboard' -H 'Authorization: Bearer {tok}'")[:400])
    print("== /admin/users 状态码 ==")
    print(run(f"curl -s -m 10 -o /dev/null -w 'HTTP %{{http_code}}' 'http://127.0.0.1:8082/api/v1/admin/users?page=1&size=2' -H 'Authorization: Bearer {tok}'"))
ssh.close()
print()
print("DONE")
