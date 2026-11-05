# -*- coding: utf-8 -*-
"""确认 systemd ExecStart 与当前 jar 时间戳"""
import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)
cmds = [
    ("ExecStart", "grep -A3 ExecStart /etc/systemd/system/btr.service"),
    ("jar时间", "ls -la /root/BRT/target/btr-platform.jar"),
    ("mvn存在", "which mvn && mvn -v 2>/dev/null | head -2"),
    ("工作目录", "grep -E 'WorkingDirectory|User=' /etc/systemd/system/btr.service"),
]
for name, cmd in cmds:
    i, o, e = ssh.exec_command(cmd, timeout=60)
    print(f"=== {name} ===\n{o.read().decode('utf-8','replace').strip()}\n")
ssh.close()
print("DONE")
