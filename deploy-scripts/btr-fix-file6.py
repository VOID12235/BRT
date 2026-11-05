# -*- coding: utf-8 -*-
"""修复 upload_file id=6 md5/original_name，删除重复 id=7"""
import paramiko
ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)
def run(c, timeout=30):
    i, o, e = ssh.exec_command(c, timeout=timeout)
    return o.read().decode("utf-8", "replace").strip()
print(run("mysql -ubtr -pbtr_pass btr -e \"UPDATE upload_file SET md5='5f6d7a8b9c0d1e2f3a4b5c6d7e8f9a0b', original_name='青春纪念册-作品文件.pdf' WHERE id=6; DELETE FROM upload_file WHERE id=7; SELECT id,original_name,md5,url FROM upload_file WHERE id=6\\G\" 2>&1"))
ssh.close()
print("DONE")
