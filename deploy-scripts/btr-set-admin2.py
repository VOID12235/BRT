# -*- coding: utf-8 -*-
import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

def run(c):
    i, o, e = ssh.exec_command(c, timeout=30)
    return o.read().decode("utf-8", "replace").strip()

print("== 全量用户 ==")
print(run("mysql -h127.0.0.1 -P3306 -ubtr -pbtr_pass btr -e \"SELECT id, email, nickname, role, status FROM sys_user\" 2>&1"))
print("== 模糊匹配 163 ==")
print(run("mysql -h127.0.0.1 -P3306 -ubtr -pbtr_pass btr -e \"SELECT id, email, nickname, role FROM sys_user WHERE email LIKE '%163%' OR email LIKE '%a3614%'\" 2>&1"))
ssh.close()
print("DONE")
