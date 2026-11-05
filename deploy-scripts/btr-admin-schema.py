# -*- coding: utf-8 -*-
import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

def run(c):
    i, o, e = ssh.exec_command(c, timeout=30)
    return o.read().decode("utf-8", "replace").strip()

print("== 表结构 ==")
print(run("mysql -h127.0.0.1 -P3306 -ubtr -pbtr_pass btr -e \"SHOW CREATE TABLE sys_user\\G\" 2>&1"))
print("== admin 哈希前缀 ==")
print(run("mysql -h127.0.0.1 -P3306 -ubtr -pbtr_pass btr -N -e \"SELECT id, LEFT(password,7), LENGTH(password) FROM sys_user WHERE id=1\" 2>&1"))
ssh.close()
print("DONE")
