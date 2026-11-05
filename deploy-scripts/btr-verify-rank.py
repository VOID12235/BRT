# -*- coding: utf-8 -*-
"""验证重启后榜单接口"""
import paramiko, json, time

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

def run(cmd, timeout=30):
    i, o, e = ssh.exec_command(cmd, timeout=timeout)
    return o.read().decode("utf-8", "replace").strip()

print("== 进程 ==")
print(run("ps aux | grep btr-platform | grep -v grep | awk '{print $2, $9}'") or "(无进程)")
print("== 健康 ==")
print(run("curl -s -m 5 -o /dev/null -w 'HTTP %{http_code}' http://127.0.0.1:8082/api/v1/health"))
print()
time.sleep(3)
print("== /ranks?contestId=3 ==")
out = run("curl -s -m 8 'http://127.0.0.1:8082/api/v1/ranks?contestId=3'")
try:
    d = json.loads(out)
    data = d.get("data", d)
    for r in data.get("rows", [])[:3]:
        print("  rank", r.get("rank"), r.get("workTitle"), "| author:", r.get("author"), "| school:", r.get("school"), "| fileUrl:", r.get("fileUrl"), "| fileName:", r.get("fileName"))
except Exception as e:
    print("RAW:", out[:300])
ssh.close()
print("DONE")
