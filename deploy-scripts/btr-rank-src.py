# -*- coding: utf-8 -*-
"""查看后端榜单接口字段"""
import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

def run(cmd):
    i, o, e = ssh.exec_command(cmd, timeout=40)
    return o.read().decode("utf-8", "replace").strip()

cmds = [
    "grep -rln 'ranks\\|RankRow\\|record RankRow' /root/BRT/src/main/java/com/btr/platform/ | head -8",
    "grep -rn '匿名\\|realName\\|author\\|nickname' /root/BRT/src/main/java/com/btr/platform/controller/RankController.java /root/BRT/src/main/java/com/btr/platform/service/RankService.java 2>/dev/null | head -20",
]
for c in cmds:
    print("== " + c[:80])
    print(run(c) or "(空)")
    print()
ssh.close()
print("DONE")
