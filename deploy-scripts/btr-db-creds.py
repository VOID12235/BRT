# -*- coding: utf-8 -*-
"""读取 java 进程环境变量中的 DB 凭据，然后查用户表（只读）"""
import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

def run(cmd):
    i, o, e = ssh.exec_command(cmd, timeout=40)
    return o.read().decode("utf-8", "replace").strip()

cmds = [
    "tr '\\0' '\\n' < /proc/131217/environ 2>/dev/null | grep -E '^DB_|^SPRING_PROFILES|^JAVA' | head -12",
    "cat /root/BRT/deploy/.env 2>/dev/null | grep -E 'DB_|MYSQL' | head -10; ls /root/BRT/deploy 2>/dev/null | head",
]
for c in cmds:
    print("== " + c[:70])
    print(run(c) or "(空)")
    print()
ssh.close()
print("DONE")
