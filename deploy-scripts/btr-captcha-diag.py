# -*- coding: utf-8 -*-
import paramiko

cli = paramiko.SSHClient()
cli.set_missing_host_key_policy(paramiko.AutoAddPolicy())
cli.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)
cmds = [
    ("curl captcha direct", "curl -s -o /dev/null -w '%{http_code} %{time_total}s' http://127.0.0.1:8082/api/v1/auth/captcha"),
    ("curl captcha via nginx", "curl -s -o /dev/null -w '%{http_code} %{time_total}s' http://127.0.0.1:20757/api/v1/auth/captcha"),
    ("nginx err tail", "tail -20 /var/log/nginx/error.log 2>/dev/null"),
    ("nginx access captcha", "grep -c captcha /var/log/nginx/access.log 2>/dev/null | tail -1"),
]
for name, cmd in cmds:
    i, o, e = cli.exec_command(cmd, timeout=40)
    out = o.read().decode("utf-8", "replace").strip()
    print("===", name, "===")
    print(out[:800])
cli.close()
