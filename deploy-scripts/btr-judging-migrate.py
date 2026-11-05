# -*- coding: utf-8 -*-
"""执行打分中状态迁移 SQL"""
import paramiko

cli = paramiko.SSHClient()
cli.set_missing_host_key_policy(paramiko.AutoAddPolicy())
cli.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)
sql = (
    "UPDATE contest SET status = 'JUDGING', updated_at = NOW() "
    "WHERE status = 'REVIEW' AND review_start IS NOT NULL AND review_start <= NOW(); "
    "UPDATE work w JOIN contest c ON w.contest_id = c.id SET w.locked = TRUE "
    "WHERE c.status = 'JUDGING' AND c.mode = 'ONLINE'; "
    "SELECT id, title, status, mode FROM contest ORDER BY id;"
)
cmd = 'mysql -ubtr -pbtr_pass btr -t -e "' + sql + '" 2>/dev/null'
stdin, stdout, stderr = cli.exec_command(cmd, timeout=60)
print(stdout.read().decode("utf-8", "replace"))
err = stderr.read().decode("utf-8", "replace")
if err.strip():
    print("STDERR:", err[:300])
cli.close()
