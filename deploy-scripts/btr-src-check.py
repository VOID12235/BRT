# -*- coding: utf-8 -*-
"""检查服务器 Java 源码与 jar 内 class"""
import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

def run(cmd):
    i, o, e = ssh.exec_command(cmd, timeout=40)
    return o.read().decode("utf-8", "replace") + e.read().decode("utf-8", "replace")

cmds = [
    "grep -rl 'videoUrl' /root/BRT/src 2>/dev/null | head -10",
    "ls -l --time-style=+%m-%d\\ %H:%M /root/BRT/src/main/java/com/btr/platform/controller/ContestController.java /root/BRT/src/main/java/com/btr/platform/entity/Contest.java 2>/dev/null",
    "unzip -l /root/BRT/target/btr-platform.jar | grep -E 'Contest(Controller|Service)?\\.class|entity/Contest' | head -8",
    "unzip -p /root/BRT/target/btr-platform.jar BOOT-INF/classes/com/btr/platform/entity/Contest.class 2>/dev/null | strings | grep -i 'video' | head -5",
]
for cmd in cmds:
    print("== " + cmd[:80])
    print(run(cmd).strip() or "(空)")
    print()
ssh.close()
print("DONE")
