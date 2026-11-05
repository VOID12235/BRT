# -*- coding: utf-8 -*-
"""查看 RankService 缓存/固化逻辑"""
import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

def run(cmd):
    i, o, e = ssh.exec_command(cmd, timeout=40)
    return o.read().decode("utf-8", "replace").strip()

cmds = [
    "sed -n '1,120p' /root/BRT/src/main/java/com/btr/platform/service/RankService.java",
    "grep -n 'cache\\|evict\\|Redis\\|CacheService' /root/BRT/src/main/java/com/btr/platform/service/RankService.java | head -15",
]
for c in cmds:
    print("== " + c[:70])
    print(run(c) or "(空)")
    print()
ssh.close()
print("DONE")
