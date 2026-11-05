# -*- coding: utf-8 -*-
import paramiko

cli = paramiko.SSHClient()
cli.set_missing_host_key_policy(paramiko.AutoAddPolicy())
cli.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)
cmds = [
    ("public self 20757", "curl -s -o /dev/null -w '%{http_code} %{time_total}s' --max-time 15 http://103.236.92.193:20757/api/v1/health"),
    ("public self 8080", "curl -s -o /dev/null -w '%{http_code} %{time_total}s' --max-time 15 http://103.236.92.193:8080/api/v1/health"),
    ("localhost 20757", "curl -s -o /dev/null -w '%{http_code} %{time_total}s' --max-time 15 http://127.0.0.1:20757/api/v1/health"),
]
for name, cmd in cmds:
    i, o, e = cli.exec_command(cmd, timeout=30)
    print(name, "=>", o.read().decode("utf-8", "replace").strip())
cli.close()
