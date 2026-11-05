# -*- coding: utf-8 -*-
import paramiko

cli = paramiko.SSHClient()
cli.set_missing_host_key_policy(paramiko.AutoAddPolicy())
cli.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)
cmds = [
    ("nginx health", "curl -s -o /dev/null -w '%{http_code} %{time_total}s' http://127.0.0.1:20757/api/v1/health"),
    ("nginx login", "curl -s -o /dev/null -w '%{http_code} %{time_total}s' -X POST -H 'Content-Type: application/json' -d '{\"account\":\"x\",\"password\":\"y\"}' http://127.0.0.1:20757/api/v1/auth/login"),
    ("nginx conf upstream", "grep -n -A5 'proxy_pass\\|upstream\\|8082' /etc/nginx/conf.d/*.conf /etc/nginx/nginx.conf 2>/dev/null | head -30"),
    ("nginx error fresh", "tail -5 /var/log/nginx/error.log"),
]
for name, cmd in cmds:
    i, o, e = cli.exec_command(cmd, timeout=40)
    out = o.read().decode("utf-8", "replace").strip()
    print("===", name, "===")
    print(out[:1200])
cli.close()
