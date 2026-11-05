# -*- coding: utf-8 -*-
import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

def run(c):
    i, o, e = ssh.exec_command(c, timeout=30)
    return o.read().decode("utf-8", "replace").strip()

print(run("mv /etc/nginx/sites-enabled/btr.bak /etc/nginx/btr.bak && echo moved"))
print(run("ls /etc/nginx/sites-enabled/"))
print("== nginx -t ==")
print(run("nginx -t 2>&1"))
print(run("nginx -s reload && echo reloaded"))
ssh.close()
print("DONE")
