# -*- coding: utf-8 -*-
"""执行数据库迁移 + 校验"""
import paramiko

cli = paramiko.SSHClient()
cli.set_missing_host_key_policy(paramiko.AutoAddPolicy())
cli.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

sftp = cli.open_sftp()
sftp.put(r"D:\Users\a3614\Documents\默认项目\deploy-scripts\btr-judge-migrate.sql", "/tmp/btr-judge-migrate.sql")
sftp.close()

cmd = (
    "mysql -ubtr -pbtr_pass btr < /tmp/btr-judge-migrate.sql 2>&1 && echo MIGRATE_OK && "
    "mysql -ubtr -pbtr_pass btr -N -e "
    '"SELECT COLUMN_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=\'btr\' AND TABLE_NAME=\'contest\' AND COLUMN_NAME IN (\'judge_per_work\',\'aggregate_mode\',\'review_deadline\',\'results_locked\'); '
    'SELECT COLUMN_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=\'btr\' AND TABLE_NAME=\'contest_score\' AND COLUMN_NAME IN (\'adjusted_score\',\'adjust_reason\',\'adjust_by\',\'adjusted_at\'); '
    'SELECT COLUMN_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=\'btr\' AND TABLE_NAME=\'contest_judge_assign\' AND COLUMN_NAME=\'assign_type\';"'
)
stdin, stdout, stderr = cli.exec_command(cmd, timeout=60)
out = stdout.read().decode("utf-8", "replace")
print(out[:1500])
cli.close()
