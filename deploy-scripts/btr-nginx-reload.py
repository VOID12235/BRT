# -*- coding: utf-8 -*-
import paramiko
import time

cli = paramiko.SSHClient()
cli.set_missing_host_key_policy(paramiko.AutoAddPolicy())
cli.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)
i, o, e = cli.exec_command("systemctl reload nginx && echo RELOADED", timeout=30)
print(o.read().decode("utf-8", "replace").strip())
time.sleep(2)
for name, cmd in [
    ("nginx health", "curl -s -o /dev/null -w '%{http_code} %{time_total}s' http://127.0.0.1:20757/api/v1/health"),
    ("nginx captcha", "curl -s -o /dev/null -w '%{http_code} %{time_total}s' http://127.0.0.1:20757/api/v1/auth/captcha"),
]:
    i, o, e = cli.exec_command(cmd, timeout=30)
    print(name, "=>", o.read().decode("utf-8", "replace").strip())
cli.close()
