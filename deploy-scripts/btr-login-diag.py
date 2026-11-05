# -*- coding: utf-8 -*-
"""诊断登录失败原因"""
import bcrypt, paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

def run(c):
    i, o, e = ssh.exec_command(c, timeout=30)
    return o.read().decode("utf-8", "replace").strip()

print("== 库里账号状态与哈希 ==")
out = run("mysql -h127.0.0.1 -P3306 -ubtr -pbtr_pass btr -N -e \"SELECT id, account, email, role, status, password FROM sys_user WHERE email='a36143614u@163.com'\" 2>&1")
print(out)
parts = out.split("\t")
if len(parts) >= 6:
    h = parts[5]
    ok = bcrypt.checkpw(b"Btr163#2026", h.encode("utf-8"))
    print("密码 Btr163#2026 匹配库中哈希:", ok)

print("== 登录失败锁定（Redis login:lock）==")
print(run("redis-cli --scan --pattern 'login:lock:*' | head -5; redis-cli --scan --pattern 'login:fail:*' | head -5"))
print("== 最近登录日志 ==")
print(run("mysql -h127.0.0.1 -P3306 -ubtr -pbtr_pass btr -e \"SELECT id, user_id, action, detail, created_at FROM sys_log WHERE action IN ('LOGIN','LOGIN_FAIL') ORDER BY id DESC LIMIT 8\" 2>&1"))
ssh.close()
print("DONE")
