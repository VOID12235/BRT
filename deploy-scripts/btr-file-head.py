# -*- coding: utf-8 -*-
import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

def run(c):
    i, o, e = ssh.exec_command(c, timeout=30)
    return o.read().decode("utf-8", "replace").strip()

print("== upload_file original_name ==")
print(run("mysql -h127.0.0.1 -P3306 -ubtr -pbtr_pass btr -e \"SELECT id, url, original_name, size FROM upload_file\" 2>&1"))
print("== 响应头 ==")
print(run("curl -sI -m 8 'http://127.0.0.1:8082/api/v1/files/2/a0b6b54611c8114c08e9a0da1957334b' | head -8"))
print("== 下载接口响应头对照 ==")
print(run("curl -sI -m 8 'http://127.0.0.1:8082/api/v1/files/2/a0b6b54611c8114c08e9a0da1957334b/download' | grep -i content-disposition"))
ssh.close()
print("DONE")
