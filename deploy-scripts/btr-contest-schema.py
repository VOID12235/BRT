# -*- coding: utf-8 -*-
"""查 contest 表结构 + PUBLISHED 赛事 published_at"""
import paramiko

cli = paramiko.SSHClient()
cli.set_missing_host_key_policy(paramiko.AutoAddPolicy())
cli.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)
cmds = [
    "mysql -ubtr -pbtr_pass btr -N -e 'SHOW COLUMNS FROM contest' 2>/dev/null | awk '{print $1, $2}'",
    "mysql -ubtr -pbtr_pass btr -N -e \"SELECT id,title,status,published_at,finalized_at,review_start,review_end,signup_end FROM contest\" 2>/dev/null",
]
for c in cmds:
    i, o, e = cli.exec_command(c, timeout=60)
    print(o.read().decode("utf-8", "replace").strip() or "(empty)")
    print("---")
cli.close()
