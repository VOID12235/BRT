# -*- coding: utf-8 -*-
"""用 sftp 上传 SQL 文件修复密码哈希（完全绕开 bash 引号/变量展开）"""
import base64, bcrypt, paramiko, os, tempfile

TARGETS = {1: "Admin@2026", 8: "Btr163#2026"}
ENC = {}
for uid, pwd in TARGETS.items():
    h = bcrypt.hashpw(pwd.encode("utf-8"), bcrypt.gensalt(10, prefix=b"2a")).decode("utf-8")
    assert h.startswith("$2a$10$"), h
    b64 = base64.b64encode(h.encode("utf-8")).decode("ascii")
    assert "$" not in b64, b64
    ENC[uid] = (uid, pwd, b64)

# 组装 SQL（单引号字符串由 Python 直接写入文件，不经 shell）
sql = "UPDATE sys_user SET password = CASE id "
for uid, pwd, b64 in ENC.values():
    sql += f"WHEN {uid} THEN FROM_BASE64('{b64}') "
sql += "ELSE password END, updated_at = NOW() WHERE id IN (" + ",".join(str(u) for u in ENC) + ");"

local_sql = os.path.join(tempfile.gettempdir(), "fix_pwd.sql")
with open(local_sql, "w", encoding="utf-8") as f:
    f.write(sql)

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)
sftp = ssh.open_sftp()
sftp.put(local_sql, "/tmp/fix_pwd.sql")
sftp.close()

def run(c):
    i, o, e = ssh.exec_command(c, timeout=30)
    return o.read().decode("utf-8", "replace").strip()

print("== 执行 ==")
print(run("mysql -h127.0.0.1 -P3306 -ubtr -pbtr_pass btr < /tmp/fix_pwd.sql 2>&1"))
print("== 修复后前缀 ==")
print(run("mysql -h127.0.0.1 -P3306 -ubtr -pbtr_pass btr -e \"SELECT id, account, LEFT(password,12) AS pfx FROM sys_user WHERE id IN (1,8)\" 2>&1"))

print("== 回读校验 ==")
out = run("mysql -h127.0.0.1 -P3306 -ubtr -pbtr_pass btr -N -e \"SELECT id, password FROM sys_user WHERE id IN (1,8)\" 2>&1")
for line in out.splitlines():
    if not line.strip() or line.startswith("mysql"):
        continue
    parts = line.split("\t")
    uid = int(parts[0])
    h = parts[1]
    pwd = ENC[uid][1]
    print(f"id={uid} 密码 '{pwd}' 匹配:", bcrypt.checkpw(pwd.encode("utf-8"), h.encode("utf-8")))
ssh.close()
print("DONE")
