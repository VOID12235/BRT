# -*- coding: utf-8 -*-
"""查看 Contest 实体 videoUrl 定义 + 加列"""
import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

def run(cmd, timeout=30):
    i, o, e = ssh.exec_command(cmd, timeout=timeout)
    return o.read().decode("utf-8", "replace").strip()

print("== Contest.java videoUrl 段 ==")
print(run("grep -n -A2 -B2 'video' /root/BRT/src/main/java/com/btr/platform/entity/Contest.java") or "(无)")
print("== contest 表现有列 ==")
print(run("mysql -h127.0.0.1 -P3306 -ubtr -pbtr_pass btr -N -e \"SHOW COLUMNS FROM contest\" | awk '{print $1, $2}'"))
ssh.close()
print("DONE")
