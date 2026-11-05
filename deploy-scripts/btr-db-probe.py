# -*- coding: utf-8 -*-
import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

def run(c):
    i, o, e = ssh.exec_command(c, timeout=30)
    return o.read().decode("utf-8", "replace").strip()

print("== 各赛事作品状态分布 ==")
print(run("mysql -h127.0.0.1 -P3306 -ubtr -pbtr_pass btr -e \"SELECT contest_id, status, COUNT(*) FROM contest_work GROUP BY contest_id, status\" 2>&1"))
print("== 获奖作品状态 ==")
print(run("mysql -h127.0.0.1 -P3306 -ubtr -pbtr_pass btr -e \"SELECT w.id, w.contest_id, w.status, w.file_url FROM contest_work w JOIN contest_winner wn ON wn.work_id=w.id\" 2>&1"))
print("== 报名状态分布 ==")
print(run("mysql -h127.0.0.1 -P3306 -ubtr -pbtr_pass btr -e \"SELECT contest_id, status, COUNT(*) FROM contest_signup GROUP BY contest_id, status\" 2>&1"))
print("== 奖项表 ==")
print(run("mysql -h127.0.0.1 -P3306 -ubtr -pbtr_pass btr -e \"SELECT id, contest_id, name, quota FROM contest_award\" 2>&1"))
ssh.close()
print("DONE")
