# -*- coding: utf-8 -*-
"""后端状态与接口验证"""
import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

cmds = [
    "ps aux | grep -E 'java|mvn' | grep -v grep | head -5",
    "find / -maxdepth 4 -name '*.jar' -newer /tmp/.marker -not -path '/proc/*' 2>/dev/null | head; ls -l --time-style=+%m-%d\ %H:%M /root/btr*.jar /root/btr/*.jar /opt/btr*.jar /opt/btr/*.jar 2>/dev/null | head -10",
    "ss -tlnp | grep -E ':8080|:8090|:20757|:8081' | head -5",
    "curl -s -m 8 http://127.0.0.1:8080/contests/1 | head -c 800",
]
for cmd in cmds:
    i, o, e = ssh.exec_command(cmd, timeout=30)
    out = o.read().decode("utf-8", "replace").strip()
    err = e.read().decode("utf-8", "replace").strip()
    print("== " + cmd)
    print(out if out else "(空)" + ((" | ERR: " + err[:200]) if err else ""))
    print()
ssh.close()
print("DONE")
