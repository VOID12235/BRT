# -*- coding: utf-8 -*-
"""查后端最新日志（rank 500 错误）"""
import paramiko

cli = paramiko.SSHClient()
cli.set_missing_host_key_policy(paramiko.AutoAddPolicy())
cli.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)
stdin, stdout, stderr = cli.exec_command(
    "journalctl -u btr.service --no-pager -n 120 | grep -E 'ERROR|Exception|Caused by' | tail -30",
    timeout=60)
out = stdout.read().decode("utf-8", "replace")
print(out if out.strip() else "(no error lines)")
cli.close()
