# -*- coding: utf-8 -*-
"""带验证码登录，打印完整响应"""
import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

def run(cmd):
    i, o, e = ssh.exec_command(cmd, timeout=40)
    return o.read().decode("utf-8", "replace").strip()

BASE = "http://127.0.0.1:8082/api/v1"
KEY = "ea85c502-52b5-4926-83ce-c0236c189d86"
CODE = "Q6J3"

for acc, pwd in [("admin", "admin123"), ("player", "player123")]:
    out = run(f"curl -s -m 8 -X POST '{BASE}/auth/login' -H 'Content-Type: application/json' -d '{{\"account\":\"{acc}\",\"password\":\"{pwd}\",\"captchaKey\":\"{KEY}\",\"captchaCode\":\"{CODE}\"}}'")
    print(f"{acc} => {out[:200]}")
ssh.close()
print("DONE")
