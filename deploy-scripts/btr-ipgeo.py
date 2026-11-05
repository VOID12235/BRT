# -*- coding: utf-8 -*-
"""查询服务器 IP 归属地"""
import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

for url in ["http://ip-api.com/json/103.236.92.193?lang=zh-CN",
            "http://ip-api.com/json/103.236.92.193"]:
    stdin, stdout, stderr = ssh.exec_command(
        "curl -s -m 15 '%s'; echo" % url, timeout=40)
    out = stdout.read().decode("utf-8", "replace").strip()
    print(url, "->", out[:600])

# 顺便看出口 IP（是否同 IP）
stdin, stdout, stderr = ssh.exec_command("curl -s -m 15 http://ip-api.com/json/?lang=zh-CN; echo", timeout=40)
out = stdout.read().decode("utf-8", "replace").strip()
print("OUT-IP ->", out[:600])

ssh.close()
print("DONE")
