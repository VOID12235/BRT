# -*- coding: utf-8 -*-
"""查询用户表（只读）"""
import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

def run(cmd):
    i, o, e = ssh.exec_command(cmd, timeout=40)
    return o.read().decode("utf-8", "replace").strip()

cmds = [
    "grep -E 'DB_(HOST|PORT|NAME|USER|PASSWORD)|JAVA_TOOL|SPRING_PROFILES' /proc/131217/environ 2>/dev/null | tr '\\0' '\\n' | grep -E 'DB_|SPRING' | head -10",
    "grep -E 'DB_(HOST|PORT|NAME|USER|PASSWORD)' /root/BRT/.env /root/BRT/docker-compose.yml /root/BRT/compose.yml 2>/dev/null | head -10",
    "ss -tlnp | grep 3306 | head -3",
]
for c in cmds:
    print("== " + c[:80])
    print(run(c))
    print()
ssh.close()
print("DONE")
