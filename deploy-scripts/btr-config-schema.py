# -*- coding: utf-8 -*-
"""查 sys_config 表结构和当前值"""
import paramiko

cli = paramiko.SSHClient()
cli.set_missing_host_key_policy(paramiko.AutoAddPolicy())
cli.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)
cmds = [
    "mysql -ubtr -pbtr_pass btr -N -e 'SHOW TABLES LIKE \"%config%\";' 2>/dev/null",
    "mysql -ubtr -pbtr_pass btr -N -e 'SHOW COLUMNS FROM sys_config;' 2>/dev/null",
    "mysql -ubtr -pbtr_pass btr -t -e 'SELECT * FROM sys_config LIMIT 40;' 2>/dev/null",
]
for c in cmds:
    stdin, stdout, stderr = cli.exec_command(c, timeout=60)
    out = stdout.read().decode("utf-8", "replace")
    print(">>", c[:60])
    print(out[:1200] if out.strip() else "(empty)")
cli.close()
