# -*- coding: utf-8 -*-
import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

def run(c):
    i, o, e = ssh.exec_command(c, timeout=30)
    return o.read().decode("utf-8", "replace").strip()

for t in ("sys_user", "contest_signup", "contest_work", "upload_file"):
    print(f"== {t} ==")
    print(run(f"mysql -h127.0.0.1 -P3306 -ubtr -pbtr_pass btr -e \"SHOW COLUMNS FROM {t}\" 2>&1 | awk '{{print $1, $2}}'"))
ssh.close()
print("DONE")
