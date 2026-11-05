# -*- coding: utf-8 -*-
import os, paramiko

FE = r"D:\Users\a3614\Documents\默认项目\frontend"
ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)
sftp = ssh.open_sftp()
n = 0
for name in os.listdir(os.path.join(FE, "m")):
    if name.endswith(".html"):
        sftp.put(os.path.join(FE, "m", name), "/var/www/btr/m/" + name)
        n += 1
sftp.close()

def run(c):
    i, o, e = ssh.exec_command(c, timeout=30)
    return o.read().decode("utf-8", "replace").strip()

print("上传:", n)
print("== 服务器版本分布 ==")
print(run("grep -h -o 'm.js?v=[0-9a-z]*' /var/www/btr/m/*.html | sort | uniq -c"))
print("== rank.html ==")
print(run("grep -o 'rank.js?v=[0-9a-z]*' /var/www/btr/rank.html"))
ssh.close()
print("DONE")
