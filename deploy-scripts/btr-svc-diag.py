# -*- coding: utf-8 -*-
import paramiko

cli = paramiko.SSHClient()
cli.set_missing_host_key_policy(paramiko.AutoAddPolicy())
cli.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)
cmds = [
    ("systemd active", "systemctl list-units --type=service --state=running --no-pager | head -20"),
    ("all btr-like", "systemctl list-units --all --no-pager | grep -iE 'btr|nginx|proxy|clash|frp|tunnel|socat' | head -10"),
    ("port 20757 history?", "ss -tlnp | grep 20757; echo none"),
    ("journal 20757", "journalctl --no-pager -n 200 2>/dev/null | grep -iE '20757|listen' | tail -10"),
]
for name, cmd in cmds:
    i, o, e = cli.exec_command(cmd, timeout=30)
    out = o.read().decode("utf-8", "replace").strip()
    print("===", name, "===")
    print(out[:1800])
cli.close()
