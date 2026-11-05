# -*- coding: utf-8 -*-
"""查 upload_file id=6 与 contest_work 文件地址"""
import paramiko
ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)
def run(c, timeout=30):
    i, o, e = ssh.exec_command(c, timeout=timeout)
    return o.read().decode("utf-8", "replace").strip()
print("== upload_file id=6 ==")
print(run("mysql -ubtr -pbtr_pass btr -e \"SELECT id,original_name,md5,url,purpose FROM upload_file WHERE id=6\\G\" 2>/dev/null"))
print("== work 6-11 文件引用 ==")
print(run("mysql -ubtr -pbtr_pass btr -e \"SELECT id,title,upload_file_id FROM contest_work WHERE id BETWEEN 6 AND 11\\G\" 2>/dev/null"))
print("== uploads 目录 ==")
print(run("ls -la /root/BRT/uploads/ | head -30"))
ssh.close()
print("DONE")
