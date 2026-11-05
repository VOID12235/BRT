# -*- coding: utf-8 -*-
"""把手机端 frontend/m 纳入服务器 git 仓库并提交"""
import os, paramiko

LOCAL = r"D:\Users\a3614\Documents\默认项目\frontend\m"
REMOTE = "/root/BRT/frontend/m"

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)
sftp = ssh.open_sftp()

def ensure_dir(path):
    try:
        sftp.stat(path)
    except IOError:
        ensure_dir("/".join(path.split("/")[:-1]))
        sftp.mkdir(path)

ensure_dir(REMOTE)
files = [f for f in os.listdir(LOCAL) if f.endswith((".html", ".css", ".js"))]
for f in files:
    sftp.put(os.path.join(LOCAL, f), REMOTE + "/" + f)
print("UP", len(files), "files")

sftp.close()
i, o, e = ssh.exec_command(
    "cd /root/BRT && git add -A && git commit -m 'feat(m): 手机端满血版纳入版本库——忘记密码/作品集/评委工作台/管理后台 + 细节修复' 2>&1 | tail -3", timeout=60)
print(o.read().decode("utf-8", "replace").strip())
ssh.close()
print("DONE")
