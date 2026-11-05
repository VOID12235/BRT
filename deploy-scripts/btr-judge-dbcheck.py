# -*- coding: utf-8 -*-
"""查看赛事3评审数据现状"""
import paramiko

cli = paramiko.SSHClient()
cli.set_missing_host_key_policy(paramiko.AutoAddPolicy())
cli.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)
sql = (
    "SELECT id,title,status,mode,judge_per_work,aggregate_mode,results_locked FROM contest WHERE id=3; "
    "SELECT id,judge_id,work_id,signup_id,assign_type FROM contest_judge_assign WHERE contest_id=3; "
    "SELECT id,contest_id,work_id,signup_id,judge_id,score,adjusted_score FROM contest_score WHERE contest_id=3; "
    "SELECT id,name,quota,sort_order FROM contest_award WHERE contest_id=3; "
    "SELECT id,award_id,work_id,signup_id,rank_no FROM contest_winner WHERE contest_id=3; "
    "SELECT id,account,nickname,role,status FROM sys_user WHERE role='JUDGE';"
)
cmd = "mysql -ubtr -pbtr_pass btr -t -e \"" + sql + "\" 2>/dev/null"
stdin, stdout, stderr = cli.exec_command(cmd, timeout=60)
print(stdout.read().decode("utf-8", "replace"))
cli.close()
