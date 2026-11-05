# -*- coding: utf-8 -*-
import json, paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

def run(c):
    i, o, e = ssh.exec_command(c, timeout=30)
    return o.read().decode("utf-8", "replace").strip()

for cid in (1, 2, 3, 4):
    raw = run(f"redis-cli get 'rank:contest:{cid}'")
    print(f"== contest {cid} 缓存 len={len(raw)} ==")
    if raw:
        try:
            d = json.loads(raw)
            rows = d.get("data", {}).get("rows") or d.get("rows") or []
            print("rows:", len(rows))
            for r in rows[:8]:
                print("  ", {k: r.get(k) for k in ("rank","awardName","workTitle","author","workId","signupId","avgScore","fileUrl") if r.get(k) is not None})
        except Exception as e:
            print("parse err", e, raw[:200])
    else:
        print("  (空)")
ssh.close()
print("DONE")
