# -*- coding: utf-8 -*-
"""查赛事状态 + 实测 works 接口"""
import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

def run(cmd):
    i, o, e = ssh.exec_command(cmd, timeout=40)
    return o.read().decode("utf-8", "replace").strip()

mysql = "mysql -h127.0.0.1 -P3306 -ubtr -pbtr_pass btr -N -e"
print("== contest 状态 ==")
print(run(f"{mysql} \"SELECT id, title, mode, status, is_public FROM contest ORDER BY id\"") or "(空)")
print()
print("== 实测 /works（PUBLISHED/FINISHED 赛事作品）==")
for cid in range(1, 7):
    out = run(f"curl -s -m 8 'http://127.0.0.1:8082/api/v1/works?contestId={cid}&page=1&size=5'")
    # 提取 author/school/title
    import json
    try:
        d = json.loads(out)
        recs = d.get("data", {}).get("records", [])
        if recs:
            print(f"contest {cid}: " + " | ".join([f"{r.get('author')}/{r.get('school')}" for r in recs[:5]]))
        else:
            print(f"contest {cid}: (无作品)")
    except Exception as ex:
        print(f"contest {cid}: 解析失败 {out[:80]}")
ssh.close()
print("DONE")
