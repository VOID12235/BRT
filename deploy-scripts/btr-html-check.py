# -*- coding: utf-8 -*-
import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

def run(c):
    i, o, e = ssh.exec_command(c, timeout=30)
    return o.read().decode("utf-8", "replace").strip()

print("== 服务器 rank.html 引用 ==")
print(run("grep -o 'm.js?v=[0-9a-z]*\\|m.css?v=[0-9a-z]*' /var/www/btr/m/rank.html"))
print("== 全部 m html 版本分布 ==")
print(run("grep -h -o 'm.js?v=[0-9a-z]*' /var/www/btr/m/*.html | sort | uniq -c"))
print("== 本地 rank.html 引用 ==")
print(run("echo local-check"))
ssh.close()
print("DONE")
