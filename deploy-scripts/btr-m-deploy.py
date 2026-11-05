# -*- coding: utf-8 -*-
"""部署手机端满血版：上传 frontend/m 全部文件到服务器 /var/www/btr/m/"""
import os, paramiko

LOCAL = r"D:\Users\a3614\Documents\默认项目\frontend\m"
REMOTE = "/var/www/btr/m"

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)
sftp = ssh.open_sftp()

files = [f for f in os.listdir(LOCAL) if f.endswith((".html", ".css", ".js"))]
files.sort()
for f in files:
    lp = os.path.join(LOCAL, f)
    sftp.put(lp, REMOTE + "/" + f)
    print("UP", f, os.path.getsize(lp))

sftp.close()
i, o, e = ssh.exec_command("ls /var/www/btr/m/ | wc -l && ls /var/www/btr/m/ | grep -E 'forgot|portfolio|judge|admin'", timeout=30)
print(o.read().decode("utf-8", "replace").strip())
ssh.close()
print("DONE")
