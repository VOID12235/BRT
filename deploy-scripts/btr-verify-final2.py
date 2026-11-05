# -*- coding: utf-8 -*-
"""清榜单缓存并验证"""
import paramiko, json

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

def run(c):
    i, o, e = ssh.exec_command(c, timeout=30)
    return o.read().decode("utf-8", "replace").strip()

print("== 清缓存 ==")
print(run("redis-cli --scan --pattern 'rank:contest:*' | xargs -r redis-cli del; echo cleared"))
print("== 重取 /ranks ==")
out = run("curl -s -m 8 'http://127.0.0.1:8082/api/v1/ranks?contestId=3'")
d = json.loads(out)
for r in d["data"]["rows"]:
    print(" ", r.get("rank"), r.get("workTitle"), "|", r.get("author"), "|", r.get("school"), "| fileUrl:", r.get("fileUrl"), "| fileName:", r.get("fileName"))
print("== contest 4 ==")
out2 = run("curl -s -m 8 'http://127.0.0.1:8082/api/v1/ranks?contestId=4'")
d2 = json.loads(out2)
for r in d2["data"]["rows"]:
    print(" ", r.get("rank"), r.get("workTitle"), "|", r.get("author"), "|", r.get("school"), "| fileUrl:", r.get("fileUrl"))
ssh.close()
print("DONE")
