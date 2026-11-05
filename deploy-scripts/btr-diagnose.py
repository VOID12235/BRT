# -*- coding: utf-8 -*-
"""诊断构建工具与进程"""
import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

def run(cmd, timeout=30):
    i, o, e = ssh.exec_command(cmd, timeout=timeout)
    return o.read().decode("utf-8", "replace").strip()

cmds = [
    "ls /root/BRT/ | head -20",
    "which mvn mvnw 2>/dev/null; ls /root/BRT/*.xml /root/BRT/pom.xml 2>/dev/null; ls /usr/share/maven/bin/mvn /opt/*/mvn 2>/dev/null; ls /root/.m2 2>/dev/null | head -3",
    "ps aux | grep -E 'java|mvn' | grep -v grep | head -6",
    "tail -20 /root/BRT/btr.log 2>/dev/null",
    "cat /root/BRT/Dockerfile 2>/dev/null | head -30; ls /root/BRT/docker* /root/BRT/*.sh 2>/dev/null",
]
for c in cmds:
    print("== " + c[:70])
    print(run(c) or "(空)")
    print()
ssh.close()
print("DONE")
