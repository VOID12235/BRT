# -*- coding: utf-8 -*-
"""查看 toView 调用点与 signup 获取"""
import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

def run(cmd):
    i, o, e = ssh.exec_command(cmd, timeout=40)
    return o.read().decode("utf-8", "replace").strip()

print(run("grep -n 'toView' /root/BRT/src/main/java/com/btr/platform/service/WorkService.java"))
print("----")
print(run("sed -n '60,120p' /root/BRT/src/main/java/com/btr/platform/service/WorkService.java"))
ssh.close()
print("DONE")
