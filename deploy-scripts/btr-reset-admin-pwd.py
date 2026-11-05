# -*- coding: utf-8 -*-
"""重置 admin@btr.dev 密码"""
import bcrypt, paramiko

PASSWORD = "Admin@2026"
hashed = bcrypt.hashpw(PASSWORD.encode("utf-8"), bcrypt.gensalt(10, prefix=b"2a")).decode("utf-8")

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

def run(c):
    i, o, e = ssh.exec_command(c, timeout=30)
    return o.read().decode("utf-8", "replace").strip()

print("== 重置密码 ==")
print(run(f"mysql -h127.0.0.1 -P3306 -ubtr -pbtr_pass btr -e \"UPDATE sys_user SET password='{hashed}', updated_at=NOW() WHERE id=1\" 2>&1"))
print("== 验证 ==")
print(run("mysql -h127.0.0.1 -P3306 -ubtr -pbtr_pass btr -e \"SELECT id, account, email, nickname, role, status FROM sys_user WHERE id=1\" 2>&1"))
ssh.close()

ok = bcrypt.checkpw(PASSWORD.encode("utf-8"), hashed.encode("utf-8"))
print("bcrypt match:", ok)
print("DONE")
