# -*- coding: utf-8 -*-
import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

def run(c):
    i, o, e = ssh.exec_command(c, timeout=30)
    return o.read().decode("utf-8", "replace").strip()

print("== contest video 列 ==")
print(run('mysql -h127.0.0.1 -P3306 -ubtr -pbtr_pass btr -N -e "SHOW COLUMNS FROM contest LIKE \'%video%\'"') or "(无列!)")
print("== 库列表 ==")
print(run('mysql -h127.0.0.1 -P3306 -uroot -pd8fb556992eab34f0d3cce01 -N -e "SHOW DATABASES" 2>/dev/null'))
ssh.close()
print("DONE")
