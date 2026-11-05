# -*- coding: utf-8 -*-
"""查询 sys_user 表（只读）"""
import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

def run(cmd):
    i, o, e = ssh.exec_command(cmd, timeout=40)
    return o.read().decode("utf-8", "replace").strip()

mysql = "mysql -h127.0.0.1 -P3306 -ubtr -btr_pass btr -N -e"
mysql = "mysql -h127.0.0.1 -P3306 -ubtr -pbtr_pass btr -N -e"
cmds = [
    f"{mysql} \"SHOW COLUMNS FROM sys_user\"",
    f"{mysql} \"SELECT id, account, nickname, role, status FROM sys_user ORDER BY id LIMIT 40\"",
]
for c in cmds:
    print("== " + c[:90])
    print(run(c) or "(空)")
    print()
ssh.close()
print("DONE")
