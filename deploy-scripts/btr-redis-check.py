# -*- coding: utf-8 -*-
"""查 Redis 榜单缓存"""
import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

def run(cmd):
    i, o, e = ssh.exec_command(cmd, timeout=40)
    return o.read().decode("utf-8", "replace").strip()

cmds = [
    "redis-cli -n 0 keys 'rank:*' 2>/dev/null | head; redis-cli keys 'rank:*' 2>/dev/null | head",
    "redis-cli --scan --pattern 'rank:*' 2>/dev/null | head -10",
    "grep -n 'redis' /root/BRT/src/main/resources/application-prod.yml | head -6",
]
for c in cmds:
    print("== " + c[:70])
    print(run(c) or "(空)")
    print()
ssh.close()
print("DONE")
