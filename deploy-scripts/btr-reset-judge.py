# -*- coding: utf-8 -*-
"""重置演示评委密码为 Judge123#（bcrypt，heredoc 避免 $ 展开）"""
import paramiko

cli = paramiko.SSHClient()
cli.set_missing_host_key_policy(paramiko.AutoAddPolicy())
cli.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)
h = "$2a$10$qxTJPyA4W39DGmRE7tL3tuB5TweJPiwMPyhLdmuUVIS.AqHOF6KjW"
sql = "UPDATE sys_user SET password='" + h + "' WHERE account='judge';"
sql += "SELECT account, password FROM sys_user WHERE account IN ('judge','judge2');"
cmd = "mysql -ubtr -pbtr_pass btr <<'EOF'\n" + sql + "\nEOF"
stdin, stdout, stderr = cli.exec_command(cmd, timeout=60)
print(stdout.read().decode("utf-8", "replace").strip())
cli.close()
