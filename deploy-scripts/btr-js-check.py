# -*- coding: utf-8 -*-
import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

def run(c):
    i, o, e = ssh.exec_command(c, timeout=30)
    return o.read().decode("utf-8", "replace").strip()

print("== 服务器 rank.js 是否含 workActions/下载作品 ==")
print(run("grep -c 'workActions\\|下载作品\\|downloadUrl' /var/www/btr/assets/js/rank.js"))
print(run("grep -o '下载作品\\|watchVideo\\|workActions' /var/www/btr/assets/js/rank.js | sort | uniq -c"))
print("== 时间戳 ==")
print(run("ls -l --time-style=+%H:%M:%S /var/www/btr/assets/js/rank.js"))
ssh.close()
print("DONE")
