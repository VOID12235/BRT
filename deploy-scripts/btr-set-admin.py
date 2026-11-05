# -*- coding: utf-8 -*-
import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

def run(c):
    i, o, e = ssh.exec_command(c, timeout=30)
    return o.read().decode("utf-8", "replace").strip()

print("== 查用户 ==")
print(run("mysql -h127.0.0.1 -P3306 -ubtr -pbtr_pass btr -e \"SELECT id, email, nickname, role, status FROM sys_user WHERE email='a36143614u@163.com'\" 2>&1"))
ssh.close()
print("DONE")
