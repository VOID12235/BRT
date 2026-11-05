# -*- coding: utf-8 -*-
"""读取数据库配置与用户账号（只读）"""
import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

def run(cmd):
    i, o, e = ssh.exec_command(cmd, timeout=40)
    return o.read().decode("utf-8", "replace").strip()

cmds = [
    "grep -A8 'datasource' /root/BRT/src/main/resources/application.yml /root/BRT/src/main/resources/application-prod.yml 2>/dev/null | head -20",
    "ls /root/BRT/src/main/resources/",
]
for c in cmds:
    print("== " + c[:80])
    print(run(c))
    print()
ssh.close()
print("DONE")
