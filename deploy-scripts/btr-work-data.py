# -*- coding: utf-8 -*-
"""查 work 表与用户关联（只读）"""
import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

def run(cmd):
    i, o, e = ssh.exec_command(cmd, timeout=40)
    return o.read().decode("utf-8", "replace").strip()

mysql = "mysql -h127.0.0.1 -P3306 -ubtr -pbtr_pass btr -N -e"
cmds = [
    f"{mysql} \"SHOW TABLES\"",
    f"{mysql} \"SELECT id, contest_id, user_id, signup_id, title, status FROM work WHERE status='SUBMITTED' ORDER BY contest_id LIMIT 25\"",
]
for c in cmds:
    print("== " + c[:90])
    print(run(c) or "(空)")
    print()
ssh.close()
print("DONE")
