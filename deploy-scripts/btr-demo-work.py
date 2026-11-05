# -*- coding: utf-8 -*-
"""清理 demo_li 后重插"""
import base64, os, paramiko, tempfile

MD5 = "5f6d7a8b9c0d1e2f3a4b5c6d7e8f9a0b"
FID = 6
pwd_b64 = base64.b64encode(b"$2a$10$demodemo01").decode()

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

def run(c):
    i, o, e = ssh.exec_command(c, timeout=30)
    return o.read().decode("utf-8", "replace").strip()

print("== 清理残留 ==")
print(run("mysql -h127.0.0.1 -P3306 -ubtr -pbtr_pass btr -e \"DELETE FROM sys_user WHERE account='demo_li'\" 2>&1"))

sql = f"""INSERT INTO sys_user (account, email, nickname, password, role, status, token_version, created_at, updated_at)
VALUES ('demo_li', 'demo_li@example.com', '李雷', FROM_BASE64('{pwd_b64}'), 'PLAYER', 'ACTIVE', 0, NOW(), NOW());
SET @uid = LAST_INSERT_ID();
INSERT INTO contest_signup (contest_id, user_id, real_name, school, status, created_at)
VALUES (3, @uid, '李雷', '山东科技大学', 'APPROVED', NOW());
SET @sid = LAST_INSERT_ID();
INSERT INTO upload_file (url, purpose, created_at)
VALUES ('/api/v1/files/{FID}/{MD5}', 'WORK', NOW());
INSERT INTO contest_work (contest_id, user_id, signup_id, title, status, file_url, file_name, cover, submitted_at, updated_at)
VALUES (3, @uid, @sid, '青春纪念册', 'SUBMITTED', '/api/v1/files/{FID}/{MD5}', 'demo.pdf', NULL, NOW(), NOW());
"""
lp = os.path.join(tempfile.gettempdir(), "demo_work.sql")
with open(lp, "w", encoding="utf-8") as f:
    f.write(sql)
sftp = ssh.open_sftp()
sftp.put(lp, "/tmp/demo_work.sql")
sftp.close()
print("== 重插 ==")
print(run("mysql -h127.0.0.1 -P3306 -ubtr -pbtr_pass btr < /tmp/demo_work.sql 2>&1"))
print("== 校验 ==")
print(run("mysql -h127.0.0.1 -P3306 -ubtr -pbtr_pass btr -e \"SELECT w.id, w.title, s.real_name, s.school FROM contest_work w JOIN contest_signup s ON s.id=w.signup_id WHERE w.contest_id=3\" 2>&1"))
ssh.close()
print("DONE")
