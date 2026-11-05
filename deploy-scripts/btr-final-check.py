# -*- coding: utf-8 -*-
import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

def run(c):
    i, o, e = ssh.exec_command(c, timeout=30)
    return o.read().decode("utf-8", "replace").strip()

print("== java 进程 ==")
print(run("ps aux | grep -E 'java.*btr-platform' | grep -v grep | awk '{print $2, $9, $11}'"))
print("== 健康检查 ==")
print(run('curl -s -m 5 -o /dev/null -w "HTTP %{http_code}" http://127.0.0.1:8082/api/v1/health'))
print()
ssh.close()
print("DONE")
