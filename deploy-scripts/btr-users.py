# -*- coding: utf-8 -*-
"""确认 judge 密码 hash 已更新"""
import paramiko

cli = paramiko.SSHClient()
cli.set_missing_host_key_policy(paramiko.AutoAddPolicy())
cli.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)
sql = "SELECT account, password FROM sys_user WHERE account IN ('judge','judge2');"
cmd = "mysql -ubtr -pbtr_pass btr -e \"" + sql + "\" 2>/dev/null"
stdin, stdout, stderr = cli.exec_command(cmd, timeout=60)
print(stdout.read().decode("utf-8", "replace").strip())
cli.close()
