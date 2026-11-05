# -*- coding: utf-8 -*-
"""验证新接口字段与状态机"""
import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)
cmds = [
    "curl -s 'http://127.0.0.1:8080/api/v1/contests/1' | head -c 600",
    "curl -s -o /dev/null -w '%{http_code}\\n' http://127.0.0.1:8080/api/v1/works?contestId=1",
    "curl -s http://127.0.0.1:8080/api/v1/contests/1 | grep -o '\"videoUrl\"[^,]*\\|\"videoName\"[^,]*'",
]
for c in cmds:
    i, o, e = ssh.exec_command(c, timeout=30)
    print(o.read().decode("utf-8", "replace").strip()[:600])
    err = e.read().decode("utf-8", "replace").strip()
    if err: print("ERR:", err[:200])
ssh.close()
print("DONE")
