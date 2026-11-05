# -*- coding: utf-8 -*-
"""创建 a36143614u@163.com 管理员账号"""
import bcrypt, paramiko

PASSWORD = "Btr163#2026"
EMAIL = "a36143614u@163.com"
ACCOUNT = "a36143614u"
NICKNAME = "a36143614u"

# BCrypt $2a$10$（与 Spring BCryptPasswordEncoder 默认一致）
hashed = bcrypt.hashpw(PASSWORD.encode("utf-8"), bcrypt.gensalt(10, prefix=b"2a")).decode("utf-8")
print("hash:", hashed)

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

def run(c):
    i, o, e = ssh.exec_command(c, timeout=30)
    return o.read().decode("utf-8", "replace").strip()

sql = f"INSERT INTO sys_user (account, password, nickname, email, role, status, created_at, updated_at) VALUES ('{ACCOUNT}', '{hashed}', '{NICKNAME}', '{EMAIL}', 'ADMIN', 'ACTIVE', NOW(), NOW())"
print(run(f"mysql -h127.0.0.1 -P3306 -ubtr -pbtr_pass btr -e \"{sql}\" 2>&1"))
print("== 验证 ==")
print(run(f"mysql -h127.0.0.1 -P3306 -ubtr -pbtr_pass btr -e \"SELECT id, account, email, nickname, role, status FROM sys_user WHERE email='{EMAIL}'\" 2>&1"))
ssh.close()
print("DONE")
