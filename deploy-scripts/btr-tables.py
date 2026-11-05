# -*- coding: utf-8 -*-
"""查 work 表结构（带错误输出）"""
import paramiko

cli = paramiko.SSHClient()
cli.set_missing_host_key_policy(paramiko.AutoAddPolicy())
cli.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)
cmd = 'mysql -ubtr -pbtr_pass btr -e "SHOW TABLES"'
stdin, stdout, stderr = cli.exec_command(cmd, timeout=60)
out = stdout.read().decode("utf-8", "replace").strip()
err = stderr.read().decode("utf-8", "replace").strip()
print("OUT:", out[:500] if out else "(empty)")
print("ERR:", err[:300] if err else "(none)")
cli.close()
