# -*- coding: utf-8 -*-
import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

def run(c):
    i, o, e = ssh.exec_command(c, timeout=30)
    return o.read().decode("utf-8", "replace").strip()

print("== nginx log_format ==")
print(run("grep -rn 'log_format' /etc/nginx/ 2>/dev/null | head -5"))
print("== access.log 最近 20 行（含时间/上游）==")
print(run("tail -20 /var/log/nginx/access.log"))
ssh.close()
print("DONE")
