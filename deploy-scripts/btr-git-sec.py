# -*- coding: utf-8 -*-
"""服务器 git 提交本轮安全修复"""
import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)
i, o, e = ssh.exec_command(
    "cd /root/BRT && git add -A && git commit -m 'security: IP解析只信X-Real-IP、移除uploads静态映射、8082收口127.0.0.1' 2>&1 | tail -3", timeout=60)
print(o.read().decode("utf-8", "replace").strip())
ssh.close()
print("DONE")
