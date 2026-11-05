# -*- coding: utf-8 -*-
"""部署更新版本的 html"""
import os, paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)
sftp = ssh.open_sftp()

LOCAL = r"D:\Users\a3614\Documents\默认项目\frontend"
FILES = [
    (LOCAL + r"\rank.html", "/var/www/btr/rank.html"),
    (LOCAL + r"\contest-detail.html", "/var/www/btr/contest-detail.html"),
]
for f in os.listdir(LOCAL + r"\m"):
    if f.endswith(".html"):
        FILES.append((LOCAL + r"\m" + os.sep + f, "/var/www/btr/m/" + f))

for lp, rp in FILES:
    sftp.put(lp, rp)
    print("UP", rp)
sftp.close()
ssh.close()
print("DONE")
