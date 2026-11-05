# -*- coding: utf-8 -*-
import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

def run(c):
    i, o, e = ssh.exec_command(c, timeout=30)
    return o.read().decode("utf-8", "replace").strip()

print("== contest_work 3/4 ==")
print(run('mysql -h127.0.0.1 -P3306 -ubtr -pbtr_pass btr -N -e "SELECT id, title, file_url, file_name FROM contest_work WHERE contest_id IN (3,4)"') or "(空)")
print("== upload_file ==")
print(run('mysql -h127.0.0.1 -P3306 -ubtr -pbtr_pass btr -N -e "SELECT id, original_name, path, url FROM upload_file ORDER BY id DESC LIMIT 8"') or "(空)")
ssh.close()
print("DONE")
