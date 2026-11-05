# -*- coding: utf-8 -*-
"""验证：评委(judge)在打分中(赛事2)可访问评审任务；赛事2评委分配"""
import paramiko

cli = paramiko.SSHClient()
cli.set_missing_host_key_policy(paramiko.AutoAddPolicy())
cli.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)
sql = "SELECT contest_id, judge_id, work_id FROM contest_judge_assign ORDER BY contest_id;"
cmd = 'mysql -ubtr -pbtr_pass btr -t -e "' + sql + '" 2>/dev/null'
stdin, stdout, stderr = cli.exec_command(cmd, timeout=60)
print(stdout.read().decode("utf-8", "replace").strip())
cli.close()
