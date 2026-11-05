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

print("== 重新 ALTER（看完整输出）==")
print(run('mysql -h127.0.0.1 -P3306 -ubtr -pbtr_pass btr -e "ALTER TABLE contest ADD COLUMN video_url VARCHAR(512) NULL COMMENT \'线下赛事录像视频URL\' AFTER is_public, ADD COLUMN video_name VARCHAR(255) NULL COMMENT \'视频名称\' AFTER video_url;"'))
print("== 验证 ==")
print(run('mysql -h127.0.0.1 -P3306 -ubtr -pbtr_pass btr -N -e "SHOW COLUMNS FROM contest LIKE \'%video%\'"'))
print("== 进程端口再确认 ==")
print(run("ss -ltn | grep -E '3306|8082'"))
ssh.close()
print("DONE")
