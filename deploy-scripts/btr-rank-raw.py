# -*- coding: utf-8 -*-
"""打印 /ranks 原始 JSON"""
import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

def run(cmd):
    i, o, e = ssh.exec_command(cmd, timeout=40)
    return o.read().decode("utf-8", "replace").strip()

print(run("curl -s -m 8 'http://127.0.0.1:8082/api/v1/ranks?contestId=3'")[:2000])
ssh.close()
print("DONE")
