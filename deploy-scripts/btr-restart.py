# -*- coding: utf-8 -*-
"""重启 btr.service 并等健康"""
import paramiko, time

cli = paramiko.SSHClient()
cli.set_missing_host_key_policy(paramiko.AutoAddPolicy())
cli.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)
i, o, e = cli.exec_command("systemctl restart btr.service && sleep 25 && curl -s http://127.0.0.1:8082/api/v1/health", timeout=90)
print(o.read().decode("utf-8", "replace").strip())
cli.close()
