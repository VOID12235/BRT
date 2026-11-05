# -*- coding: utf-8 -*-
"""查 contest_work 作品文件字段"""
import paramiko

cli = paramiko.SSHClient()
cli.set_missing_host_key_policy(paramiko.AutoAddPolicy())
cli.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)
sql = (
    "SELECT id, contest_id, title, file_url, file_name, cover, status, locked "
    "FROM contest_work WHERE contest_id IN (2,3) ORDER BY contest_id, id;"
)
cmd = 'mysql -ubtr -pbtr_pass btr -t -e "' + sql + '" 2>/dev/null'
stdin, stdout, stderr = cli.exec_command(cmd, timeout=60)
print(stdout.read().decode("utf-8", "replace").strip())
cli.close()
