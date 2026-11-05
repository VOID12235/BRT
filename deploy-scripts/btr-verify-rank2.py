# -*- coding: utf-8 -*-
import json, paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

def run(c, timeout=30):
    i, o, e = ssh.exec_command(c, timeout=timeout)
    return o.read().decode("utf-8", "replace").strip()

print("== 进程/启动 ==")
print(run("ps -eo pid,etime,args | grep -E 'java.*btr-platform.jar' | grep -v grep | head -2"))
print(run("grep -E 'Tomcat started|Port 8082 was already' /root/BRT/btr.log | tail -2"))
print("== /ranks?contestId=3 ==")
out = run("curl -s -m 10 'http://127.0.0.1:8082/api/v1/ranks?contestId=3'")
try:
    d = json.loads(out)
    rows = d.get("data", {}).get("rows") or []
    print("rows:", len(rows))
    for r in rows:
        print(" ", {k: r.get(k) for k in ("rank", "awardName", "workTitle", "author", "fileUrl", "avgScore") if r.get(k) is not None})
except Exception as e:
    print("parse err", e, out[:300])
ssh.close()
print("DONE")
