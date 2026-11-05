# -*- coding: utf-8 -*-
"""对比 RankService 源码与 jar 时间戳"""
import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

def run(cmd):
    i, o, e = ssh.exec_command(cmd, timeout=40)
    return o.read().decode("utf-8", "replace").strip()

cmds = [
    "ls -l --time-style=+%m-%d\\ %H:%M /root/BRT/src/main/java/com/btr/platform/service/RankService.java /root/BRT/target/btr-platform.jar /root/BRT/src/main/java/com/btr/platform/service/WorkService.java /root/BRT/src/main/java/com/btr/platform/service/ContestService.java",
    "unzip -l /root/BRT/target/btr-platform.jar | grep -E 'RankService|WorkService\\.class' | head -5",
    "unzip -p /root/BRT/target/btr-platform.jar BOOT-INF/classes/com/btr/platform/service/RankService.class 2>/dev/null | strings | grep -E '匿名选手|realName|setAuthor' | head -8",
]
for c in cmds:
    print("== " + c[:80])
    print(run(c) or "(空)")
    print()
ssh.close()
print("DONE")
