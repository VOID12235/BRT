# -*- coding: utf-8 -*-
"""查看 WorkService 作品列表 signup 逻辑"""
import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

def run(cmd):
    i, o, e = ssh.exec_command(cmd, timeout=40)
    return o.read().decode("utf-8", "replace").strip()

print(run("sed -n '195,245p' /root/BRT/src/main/java/com/btr/platform/service/WorkService.java"))
ssh.close()
print("DONE")
