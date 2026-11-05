# -*- coding: utf-8 -*-
"""验证 winners 生成与锁定"""
import paramiko

cli = paramiko.SSHClient()
cli.set_missing_host_key_policy(paramiko.AutoAddPolicy())
cli.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)
sql = (
    "SELECT w.id, w.award_id, a.name, w.work_id, w.rank_no FROM contest_winner w "
    "LEFT JOIN contest_award a ON w.award_id=a.id WHERE w.contest_id=3 ORDER BY w.rank_no; "
    "SELECT id, title, results_locked, finalized_at FROM contest WHERE id=3;"
)
cmd = 'mysql -ubtr -pbtr_pass btr -t -e "' + sql + '" 2>/dev/null'
stdin, stdout, stderr = cli.exec_command(cmd, timeout=60)
print(stdout.read().decode("utf-8", "replace"))
cli.close()
