# -*- coding: utf-8 -*-
import paramiko
ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)
def run(c, timeout=30):
    i, o, e = ssh.exec_command(c, timeout=timeout)
    return o.read().decode("utf-8", "replace").strip()
print("== upload_file 全部 ==")
print(run("mysql -ubtr -pbtr_pass btr -e \"SELECT id,original_name,md5,url FROM upload_file ORDER BY id\\G\" 2>&1 | head -60"))
print("== contest_work 表结构 ==")
print(run("mysql -ubtr -pbtr_pass btr -e \"DESC contest_work\" 2>&1 | head -25"))
print("== work 6-11 ==")
print(run("mysql -ubtr -pbtr_pass btr -e \"SELECT * FROM contest_work WHERE id BETWEEN 6 AND 11\\G\" 2>&1 | head -80"))
ssh.close()
print("DONE")
