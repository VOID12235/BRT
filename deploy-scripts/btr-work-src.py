# -*- coding: utf-8 -*-
"""查 /works 接口 author 逻辑"""
import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

def run(cmd):
    i, o, e = ssh.exec_command(cmd, timeout=40)
    return o.read().decode("utf-8", "replace").strip()

cmds = [
    "grep -rln 'class WorkController\\|class WorkService' /root/BRT/src/main/java/com/btr/platform/controller /root/BRT/src/main/java/com/btr/platform/service | head",
    "grep -rn 'setAuthor\\|author\\|realName\\|nickname' /root/BRT/src/main/java/com/btr/platform/service/WorkService.java | head -20",
]
for c in cmds:
    print("== " + c[:80])
    print(run(c) or "(空)")
    print()
ssh.close()
print("DONE")
