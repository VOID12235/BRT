# -*- coding: utf-8 -*-
"""确认线上版本"""
import paramiko

cli = paramiko.SSHClient()
cli.set_missing_host_key_policy(paramiko.AutoAddPolicy())
cli.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)
cmds = [
    "ls -la /root/BRT/target/btr-platform.jar | awk '{print $5, $6, $7, $8}'",
    "grep -o 'admin.js?v=20261105[a-z]*' /var/www/btr/admin/contest-manage.html | head -1",
    "grep -o 'rank.js?v=20261105[a-z]*' /var/www/btr/rank.html | head -1",
    "grep -o 'm.js?v=20261105b[0-9]*' /var/www/btr/m/index.html | head -1",
    "grep -o 'api.js?v=20261105[a-z0-9]*' /var/www/btr/admin/contest-manage.html | head -1",
    "curl -s http://127.0.0.1:8082/api/v1/health",
]
for c in cmds:
    i, o, e = cli.exec_command(c, timeout=60)
    print(o.read().decode("utf-8", "replace").strip() or "(empty)")
cli.close()
