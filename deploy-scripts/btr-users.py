# -*- coding: utf-8 -*-
"""查询用户账号（只读）"""
import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

def run(cmd):
    i, o, e = ssh.exec_command(cmd, timeout=40)
    return o.read().decode("utf-8", "replace").strip()

mysql = "mysql -h127.0.0.1 -P3306 -ubtr -pbtr_pass btr -N -e"
cmds = [
    f"{mysql} \"SHOW TABLES LIKE '%user%'\"",
    f"{mysql} \"SELECT id, account, nickname, role, status FROM user ORDER BY id LIMIT 30\" 2>/dev/null || {mysql} \"SELECT id, account, nickname, role, status FROM users ORDER BY id LIMIT 30\"",
]
for c in cmds:
    print("== " + c[:80])
    print(run(c) or "(空)")
    print()
ssh.close()
print("DONE")
