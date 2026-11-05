# -*- coding: utf-8 -*-
import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

def run(c):
    i, o, e = ssh.exec_command(c, timeout=30)
    return o.read().decode("utf-8", "replace").strip()

print("== id=8 当前状态 ==")
print(run("mysql -h127.0.0.1 -P3306 -ubtr -pbtr_pass btr -e \"SELECT id, account, email, role, status, LEFT(password,12) AS pfx FROM sys_user WHERE id=8\" 2>&1"))
print("== 全部账号状态 ==")
print(run("mysql -h127.0.0.1 -P3306 -ubtr -pbtr_pass btr -e \"SELECT id, account, role, status FROM sys_user\" 2>&1"))
print("== 最近登录相关日志 ==")
print(run("grep -E 'login|LOGIN|禁用|锁定' /root/BRT/btr.log | tail -8"))
ssh.close()
print("DONE")
