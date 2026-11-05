# -*- coding: utf-8 -*-
import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

def run(c):
    i, o, e = ssh.exec_command(c, timeout=30)
    return o.read().decode("utf-8", "replace").strip()

print("== 日志尾部 30 行 ==")
print(run("tail -30 /root/BRT/btr.log"))
print("== 进程与端口 ==")
print(run("ps -eo pid,etime,args | grep -E 'java.*btr-platform.jar' | grep -v grep"))
print(run("ss -tlnp | grep 8082"))
ssh.close()
print("DONE")
