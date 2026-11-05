# -*- coding: utf-8 -*-
"""查 prod 配置与启动方式"""
import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

def run(cmd, timeout=30):
    i, o, e = ssh.exec_command(cmd, timeout=timeout)
    return o.read().decode("utf-8", "replace").strip()

cmds = [
    "cat /root/BRT/src/main/resources/application-prod.yml",
    "ls /root/BRT/scripts/ /root/BRT/deploy 2>/dev/null; grep -rn 'DB_HOST\\|REDIS_HOST\\|127.0.0.1' /root/BRT/scripts/ /root/BRT/deploy 2>/dev/null | head -10",
    "grep -n 'SPRING_PROFILES_ACTIVE\\|DB_HOST\\|REDIS_HOST\\|btr-platform' ~/.bashrc ~/.profile /etc/environment 2>/dev/null | head -8",
]
for c in cmds:
    print("== " + c[:70])
    print(run(c) or "(空)")
    print()
ssh.close()
print("DONE")
