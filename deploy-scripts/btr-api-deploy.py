# -*- coding: utf-8 -*-
"""api.js a3 全量 html 部署 v2"""
import os, paramiko

FE = r"D:\Users\a3614\Documents\默认项目\frontend"

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)
sftp = ssh.open_sftp()

def up(rel):
    p = os.path.join(FE, rel)
    if os.path.exists(p):
        sftp.put(p, "/var/www/btr/" + rel)
        return True
    return False

n = 0
for name in os.listdir(os.path.join(FE, "m")):
    if name.endswith(".html"):
        up(os.path.join("m", name))
        n += 1
root_files = [f for f in os.listdir(FE) if f.endswith(".html")]
for name in root_files:
    up(name)
    n += 1
sftp.close()

def run(c):
    i, o, e = ssh.exec_command(c, timeout=30)
    return o.read().decode("utf-8", "replace").strip()

print("上传 html:", n)
print("== 服务器 api.js 版本分布 ==")
print(run("grep -r -h -o 'api.js?v=[0-9a-z]*' /var/www/btr --include='*.html' | sort | uniq -c"))
ssh.close()
print("DONE")
