# -*- coding: utf-8 -*-
import paramiko

cli = paramiko.SSHClient()
cli.set_missing_host_key_policy(paramiko.AutoAddPolicy())
cli.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)
cmds = [
    ("btr.bak listen", "grep -n 'listen\\|20757' /etc/nginx/btr.bak 2>/dev/null | head -8"),
    ("bak-20261104 listen", "grep -n 'listen\\|20757' /etc/nginx/sites-available/btr.bak-20261104 2>/dev/null | head -8"),
    ("current btr first 60", "head -60 /etc/nginx/sites-available/btr"),
    ("nginx conf http block", "grep -n 'include\\|server' /etc/nginx/nginx.conf | head -12"),
]
for name, cmd in cmds:
    i, o, e = cli.exec_command(cmd, timeout=30)
    out = o.read().decode("utf-8", "replace").strip()
    print("===", name, "===")
    print(out[:2000])
cli.close()
