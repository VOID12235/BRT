# -*- coding: utf-8 -*-
import paramiko

cli = paramiko.SSHClient()
cli.set_missing_host_key_policy(paramiko.AutoAddPolicy())
cli.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)
cmds = [
    ("all listens", "ss -tlnp | grep LISTEN"),
    ("20757 procs", "ps aux | grep -iE '20757|clash|socat|frp|rinetd|haproxy|nginx' | grep -v grep | head -15"),
    ("docker?", "docker ps 2>/dev/null | head -10"),
    ("clash?", "ps aux | grep -i clash | grep -v grep | head -5"),
]
for name, cmd in cmds:
    i, o, e = cli.exec_command(cmd, timeout=30)
    out = o.read().decode("utf-8", "replace").strip()
    print("===", name, "===")
    print(out[:2000])
cli.close()
