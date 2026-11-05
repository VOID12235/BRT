# -*- coding: utf-8 -*-
import paramiko

cli = paramiko.SSHClient()
cli.set_missing_host_key_policy(paramiko.AutoAddPolicy())
cli.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)
cmds = [
    ("listen in btr conf", "grep -n 'listen\\|server_name' /etc/nginx/sites-available/btr"),
    ("sites-enabled", "ls -l /etc/nginx/sites-enabled/"),
    ("nginx -T listen", "nginx -T 2>/dev/null | grep -n 'listen ' | head -10"),
    ("who listens 20757 before?", "ss -tlnp | grep -E '2075[0-9]|8082|80 '"),
]
for name, cmd in cmds:
    i, o, e = cli.exec_command(cmd, timeout=30)
    out = o.read().decode("utf-8", "replace").strip()
    print("===", name, "===")
    print(out[:1400])
cli.close()
