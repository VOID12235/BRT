# -*- coding: utf-8 -*-
"""检查构建状态"""
import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)
cmds = [
    ("mvn进程", "ps aux | grep -E 'mvn|java' | grep -v grep | head -5"),
    ("构建日志尾", "tail -40 /tmp/btr-build.log 2>/dev/null || echo NO_LOG"),
    ("日志大小", "wc -l /tmp/btr-build.log 2>/dev/null || echo 0"),
    ("jar当前", "stat -c '%y %s' /root/BRT/target/btr-platform.jar"),
    ("target目录", "ls -la /root/BRT/target/ 2>/dev/null | head -15"),
]
for name, cmd in cmds:
    i, o, e = ssh.exec_command(cmd, timeout=60)
    print(f"=== {name} ===\n{o.read().decode('utf-8','replace').strip()}\n")
ssh.close()
print("DONE")
