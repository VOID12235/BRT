# -*- coding: utf-8 -*-
"""执行赛题/公示时间迁移"""
import paramiko

cli = paramiko.SSHClient()
cli.set_missing_host_key_policy(paramiko.AutoAddPolicy())
cli.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)
sql = open(r"D:\Users\a3614\Documents\默认项目\deploy-scripts\btr-topic-migrate.sql", encoding="utf-8").read()
# heredoc 避免转义问题
cmd = "mysql -ubtr -pbtr_pass btr <<'SQLEOF'\n" + sql + "\nSQLEOF"
i, o, e = cli.exec_command(cmd, timeout=60)
err = e.read().decode("utf-8", "replace")
out = o.read().decode("utf-8", "replace")
print("OUT:", out.strip() or "(ok)")
print("ERR:", err.strip() or "(none)")
# 验证列
i2, o2, e2 = cli.exec_command(
    "mysql -ubtr -pbtr_pass btr -N -e 'SHOW COLUMNS FROM contest' 2>/dev/null | grep -E \"published_at|topic_file\"", timeout=60)
print("COLS:", o2.read().decode("utf-8", "replace").strip() or "(none)")
# 验证已公示赛事 published_at
i3, o3, e3 = cli.exec_command(
    "mysql -ubtr -pbtr_pass btr -N -e \"SELECT id,title,status,published_at FROM contest WHERE status='PUBLISHED'\" 2>/dev/null", timeout=60)
print("PUBLISHED:", o3.read().decode("utf-8", "replace").strip() or "(none)")
cli.close()
