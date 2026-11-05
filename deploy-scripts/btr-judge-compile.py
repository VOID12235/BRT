# -*- coding: utf-8 -*-
"""重跑编译查看错误"""
import paramiko

cli = paramiko.SSHClient()
cli.set_missing_host_key_policy(paramiko.AutoAddPolicy())
cli.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)
stdin, stdout, stderr = cli.exec_command(
    "cd /root/BRT && mvn -q -DskipTests compile 2>&1 | grep -E 'ERROR|BUILD FAILURE' | head -40",
    timeout=300)
out = stdout.read().decode("utf-8", "replace")
print(out if out.strip() else "NO_ERROR_OUTPUT")
cli.close()
