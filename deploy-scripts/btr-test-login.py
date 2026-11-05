# -*- coding: utf-8 -*-
"""验证码一次性，逐个账号测试"""
import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

def run(cmd):
    i, o, e = ssh.exec_command(cmd, timeout=40)
    return o.read().decode("utf-8", "replace").strip()

BASE = "http://127.0.0.1:8082/api/v1"

def login(account, password, key, code):
    out = run(f"curl -s -m 8 -X POST '{BASE}/auth/login' -H 'Content-Type: application/json' -d '{{\"account\":\"{account}\",\"password\":\"{password}\",\"captchaKey\":\"{key}\",\"captchaCode\":\"{code}\"}}'")
    return out[:120]

# 当前验证码（尚未使用）
print("player/player123 =>", login("player", "player123", "65b95331-1219-4e9d-9f40-47343c75582f", "UJEQ"))
ssh.close()
print("DONE")
