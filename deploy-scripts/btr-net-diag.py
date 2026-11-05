# -*- coding: utf-8 -*-
import paramiko

cli = paramiko.SSHClient()
cli.set_missing_host_key_policy(paramiko.AutoAddPolicy())
cli.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)
cmds = [
    ("20757 static", "curl -s -o /dev/null -w '%{http_code} %{time_total}s' http://127.0.0.1:20757/m/index.html"),
    ("20757 listen", "ss -tlnp | grep 20757"),
    ("nginx procs", "ps aux | grep '[n]ginx' | head -6"),
    ("8082 again", "curl -s -o /dev/null -w '%{http_code} %{time_total}s' http://127.0.0.1:8082/api/v1/health"),
    ("iptables 8082", "iptables -L -n 2>/dev/null | grep -i 8082 | head -5; echo ---; iptables -L OUTPUT -n 2>/dev/null | head -10"),
]
for name, cmd in cmds:
    i, o, e = cli.exec_command(cmd, timeout=30)
    out = o.read().decode("utf-8", "replace").strip()
    print("===", name, "===")
    print(out[:1200])
cli.close()
