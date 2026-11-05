# -*- coding: utf-8 -*-
"""后端 jar 与新接口行为验证"""
import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

cmds = [
    "ls -l --time-style=+%m-%d\\ %H:%M /root/BRT/target/btr-platform.jar && stat -c '%y' /root/BRT/target/btr-platform.jar",
    "curl -s -m 8 http://127.0.0.1:8082/contests/1 | head -c 1200",
    "curl -s -m 8 -o /dev/null -w 'works list HTTP %{http_code}' 'http://127.0.0.1:8082/works?contestId=1&page=1&size=5'; echo; curl -s -m 8 'http://127.0.0.1:8082/works?contestId=1&page=1&size=2' | head -c 500",
]
for cmd in cmds:
    i, o, e = ssh.exec_command(cmd, timeout=30)
    out = o.read().decode("utf-8", "replace").strip()
    err = e.read().decode("utf-8", "replace").strip()
    print("== " + cmd[:70])
    print(out if out else "(空)" + ((" | ERR: " + err[:200]) if err else ""))
    print()
ssh.close()
print("DONE")
