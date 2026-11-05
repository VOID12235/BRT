# -*- coding: utf-8 -*-
"""查启动失败原因"""
import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

def run(cmd, timeout=30):
    i, o, e = ssh.exec_command(cmd, timeout=timeout)
    return o.read().decode("utf-8", "replace").strip()

print("== 全进程 ==")
print(run("ps aux | grep -E 'java|mvn' | grep -v grep | head -10") or "(无)")
print("== btr.log 尾部 ==")
print(run("tail -40 /root/BRT/btr.log 2>/dev/null | grep -vE 'at (org|java|jdk|com.sun|sun)' | head -30") or "(无日志)")
ssh.close()
print("DONE")
