# -*- coding: utf-8 -*-
"""查 contest 表结构 + 状态值分布"""
import paramiko

cli = paramiko.SSHClient()
cli.set_missing_host_key_policy(paramiko.AutoAddPolicy())
cli.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)
cmds = [
    "mysql -ubtr -pbtr_pass btr -N -e 'SHOW COLUMNS FROM contest;' 2>/dev/null",
    "mysql -ubtr -pbtr_pass btr -t -e 'SELECT id,title,status,signup_start,signup_end FROM contest;' 2>/dev/null",
]
for c in cmds:
    stdin, stdout, stderr = cli.exec_command(c, timeout=60)
    print(">>", c[:40])
    print(stdout.read().decode("utf-8", "replace"))
cli.close()
