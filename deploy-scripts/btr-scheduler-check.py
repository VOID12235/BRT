# -*- coding: utf-8 -*-
"""查 scheduler 日志"""
import paramiko

cli = paramiko.SSHClient()
cli.set_missing_host_key_policy(paramiko.AutoAddPolicy())
cli.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)
i, o, e = cli.exec_command(
    "journalctl -u btr.service --no-pager -n 400 | grep -iE 'schedul|auto|JUDGING|SIGNUP' | tail -15", timeout=60)
print(o.read().decode("utf-8", "replace").strip() or "(no scheduler lines)")
cli.close()
