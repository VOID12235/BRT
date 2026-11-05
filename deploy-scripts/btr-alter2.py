# -*- coding: utf-8 -*-
import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

def run(c):
    i, o, e = ssh.exec_command(c, timeout=30)
    out = o.read().decode("utf-8", "replace").strip()
    err = e.read().decode("utf-8", "replace").strip()
    return out + (" | STDERR: " + err if err else "")

print("== 补列 ==")
print(run("""mysql -h127.0.0.1 -P3306 -ubtr -pbtr_pass btr -e "ALTER TABLE contest_work ADD COLUMN cover VARCHAR(255) NULL COMMENT '作品封面图' AFTER file_name; ALTER TABLE sys_user ADD COLUMN token_version INT DEFAULT 0 COMMENT 'token版本(强制下线)' AFTER avatar;" """))
print("== 验证 ==")
print(run('mysql -h127.0.0.1 -P3306 -ubtr -pbtr_pass btr -N -e "SHOW COLUMNS FROM contest_work LIKE \'%cover%\'; SHOW COLUMNS FROM sys_user LIKE \'%token%\';"'))
ssh.close()
print("DONE")
