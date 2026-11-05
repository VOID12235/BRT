# -*- coding: utf-8 -*-
"""查启动日志与端口"""
import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

def run(cmd, timeout=30):
    i, o, e = ssh.exec_command(cmd, timeout=timeout)
    return o.read().decode("utf-8", "replace").strip()

print("== 进程详情 ==")
print(run("ps aux | grep -E 'java|bash -c' | grep -v grep | head -8"))
print("== 端口 8082 ==")
print(run("ss -ltnp 2>/dev/null | grep 8082 || netstat -ltnp 2>/dev/null | grep 8082 || echo no-listen"))
print("== 日志尾部(错误段) ==")
print(run("grep -nE 'ERROR|Exception|Caused by|APPLICATION FAILED|active' /root/BRT/btr.log | tail -15"))
ssh.close()
print("DONE")
