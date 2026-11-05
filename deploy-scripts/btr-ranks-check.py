# -*- coding: utf-8 -*-
"""实测 /ranks 接口"""
import paramiko, json

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

def run(cmd):
    i, o, e = ssh.exec_command(cmd, timeout=40)
    return o.read().decode("utf-8", "replace").strip()

for cid in [3, 4]:
    out = run(f"curl -s -m 8 'http://127.0.0.1:8082/api/v1/ranks?contestId={cid}'")
    try:
        d = json.loads(out)
        data = d.get("data", d)
        print(f"contest {cid}: published={data.get('published')} status={data.get('status')} rows={len(data.get('rows', []))}")
        for r in data.get("rows", [])[:5]:
            print("   ", r.get("rank"), r.get("workTitle"), "| author:", r.get("author"), "| realName:", r.get("realName"), "| school:", r.get("school"), "| fileUrl:", r.get("fileUrl"))
    except Exception as e:
        print(f"contest {cid}: RAW {out[:200]}")
ssh.close()
print("DONE")
