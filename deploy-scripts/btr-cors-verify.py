# -*- coding: utf-8 -*-
import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

def run(c, timeout=30):
    i, o, e = ssh.exec_command(c, timeout=timeout)
    return o.read().decode("utf-8", "replace").strip()

print("== 进程 ==")
print(run("ps -eo pid,args | grep -E 'java.*btr-platform' | grep -v grep"))
print("== 端口 ==")
print(run("ss -ltnp | grep 8082 || echo no-8082"))
print("== health ==")
print(run("curl -s -m 8 http://127.0.0.1:8082/api/v1/health || echo health-fail"))
print("== 日志尾 ==")
print(run("tail -15 /root/BRT/btr.log"))
ssh.close()
print("DONE")
