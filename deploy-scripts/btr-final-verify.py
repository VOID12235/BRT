# -*- coding: utf-8 -*-
"""验证文件下载 + 榜单 fileUrl"""
import paramiko, json

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

def run(c):
    i, o, e = ssh.exec_command(c, timeout=30)
    return o.read().decode("utf-8", "replace").strip()

print("== 下载测试 ==")
print(run("curl -s -m 8 -o /dev/null -w 'HTTP %{http_code} %{size_download}B' http://127.0.0.1:8082/api/v1/files/1/4ccc27b9cad9719a62dda2a2fb3c0aaa/download"))
print()
print("== /ranks?contestId=3 ==")
out = run("curl -s -m 8 'http://127.0.0.1:8082/api/v1/ranks?contestId=3'")
d = json.loads(out)
for r in d["data"]["rows"]:
    print(" ", r.get("rank"), r.get("workTitle"), "|", r.get("author"), "|", r.get("school"), "| fileUrl:", r.get("fileUrl"))
print("== /works?contestId=3 ==")
out2 = run("curl -s -m 8 'http://127.0.0.1:8082/api/v1/works?contestId=3&page=1&size=5'")
d2 = json.loads(out2)
for r in d2.get("data", d2).get("records", [])[:3]:
    print(" ", r.get("title"), "|", r.get("author"), "| fileUrl:", r.get("fileUrl"), "| contest:", r.get("contestTitle"))
ssh.close()
print("DONE")
