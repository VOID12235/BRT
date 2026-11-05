# -*- coding: utf-8 -*-
"""查存量赛事状态与时间"""
import paramiko

cli = paramiko.SSHClient()
cli.set_missing_host_key_policy(paramiko.AutoAddPolicy())
cli.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)
sql = (
    "SELECT id, title, status, mode, signup_start, signup_end, review_start, review_end "
    "FROM contest ORDER BY id;"
)
cmd = 'mysql -ubtr -pbtr_pass btr -t -e "' + sql + '" 2>/dev/null'
stdin, stdout, stderr = cli.exec_command(cmd, timeout=60)
print(stdout.read().decode("utf-8", "replace"))
cli.close()
