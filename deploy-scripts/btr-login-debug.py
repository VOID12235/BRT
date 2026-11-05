# -*- coding: utf-8 -*-
"""查看登录返回错误"""
import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

def run(cmd):
    i, o, e = ssh.exec_command(cmd, timeout=40)
    return o.read().decode("utf-8", "replace").strip()

cmds = [
    "curl -s -m 8 -X POST 'http://127.0.0.1:8082/api/v1/auth/login' -H 'Content-Type: application/json' -d '{\"account\":\"admin\",\"password\":\"admin123\"}'",
    "curl -s -m 8 -X POST 'http://127.0.0.1:8082/api/v1/auth/login' -H 'Content-Type: application/json' -d '{\"account\":\"player\",\"password\":\"player123\"}'",
    "curl -s -m 8 'http://127.0.0.1:8082/api/v1/auth/captcha'",
]
for c in cmds:
    print("== " + c[:90])
    print(run(c))
    print()
ssh.close()
print("DONE")
