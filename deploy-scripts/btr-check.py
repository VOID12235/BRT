# -*- coding: utf-8 -*-
"""检查后端服务状态与构建"""
import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)
i, o, e = ssh.exec_command(
    "systemctl is-active btr; ls -l /root/BRT/target/btr-platform.jar 2>/dev/null; date; pgrep -f maven | head -3", timeout=30)
print(o.read().decode("utf-8", "replace").strip())
ssh.close()
print("CHECK-DONE")
