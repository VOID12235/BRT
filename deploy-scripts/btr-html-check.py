# -*- coding: utf-8 -*-
import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

def run(c):
    i, o, e = ssh.exec_command(c, timeout=30)
    return o.read().decode("utf-8", "replace").strip()

print("== 服务器 rank.html 版本 ==")
print(run("grep -o 'rank.js?v=[a-z0-9]*' /var/www/btr/rank.html"))
print("== m/rank.html 版本 ==")
print(run("grep -o 'm.js?v=[a-z0-9]*' /var/www/btr/m/rank.html"))
print("== nginx 缓存/直读 ==")
print(run("curl -s -m 5 'http://127.0.0.1:8080/rank.html' | grep -o 'rank.js?v=[a-z0-9]*'"))
ssh.close()
print("DONE")
