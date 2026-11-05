# -*- coding: utf-8 -*-
"""查看服务器仓库结构，确认前端是否纳入 git"""
import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)
for cmd in [
    "ls /root/BRT",
    "ls /root/BRT/frontend 2>/dev/null | head",
    "ls /root/BRT/frontend/m 2>/dev/null | head -3",
    "git -C /root/BRT log --oneline -6",
]:
    i, o, e = ssh.exec_command(cmd, timeout=30)
    print("==>", cmd)
    print(o.read().decode("utf-8", "replace").strip())
    err = e.read().decode("utf-8", "replace").strip()
    if err: print("ERR:", err[:200])
ssh.close()
print("DONE")
