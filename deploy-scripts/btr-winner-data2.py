# -*- coding: utf-8 -*-
"""正确查询 contest_winner"""
import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

def run(cmd):
    i, o, e = ssh.exec_command(cmd, timeout=40)
    return o.read().decode("utf-8", "replace").strip()

mysql = "mysql -h127.0.0.1 -P3306 -ubtr -pbtr_pass btr -N -e"
cmds = [
    f"{mysql} \"SELECT id, contest_id, work_id, signup_id, award_id, rank_no, published FROM contest_winner ORDER BY contest_id, rank_no LIMIT 30\"",
    f"{mysql} \"SELECT w.id, w.contest_id, w.work_id, w.signup_id, w.award_id, w.rank_no, s.real_name FROM contest_winner w LEFT JOIN contest_signup s ON s.id = w.signup_id ORDER BY w.contest_id, w.rank_no LIMIT 30\"",
]
for c in cmds:
    print("== " + c[:90])
    print(run(c) or "(空)")
    print()
ssh.close()
print("DONE")
