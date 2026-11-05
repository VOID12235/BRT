# -*- coding: utf-8 -*-
import paramiko

cli = paramiko.SSHClient()
cli.set_missing_host_key_policy(paramiko.AutoAddPolicy())
cli.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)
cmds = [
    ("listen 8082", "ss -tlnp | grep 8082"),
    ("nginx conf api", "grep -rn '8082\\|proxy_pass' /etc/nginx/ 2>/dev/null | grep -v Binary | head -10"),
    ("nginx test", "nginx -t 2>&1 | tail -3"),
    ("netstat refused?", "ss -tn | grep -c 8082"),
]
for name, cmd in cmds:
    i, o, e = cli.exec_command(cmd, timeout=30)
    out = o.read().decode("utf-8", "replace").strip()
    print("===", name, "===")
    print(out[:1000])
cli.close()
