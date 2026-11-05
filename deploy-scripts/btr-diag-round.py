# -*- coding: utf-8 -*-
import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

def run(c):
    i, o, e = ssh.exec_command(c, timeout=30)
    return o.read().decode("utf-8", "replace").strip()

print("== ERROR 详情 ==")
print(run("grep -B2 -A8 'LoggingFailureAnalysisReporter' /root/BRT/btr.log | head -20"))
print("== 进程 ==")
print(run("ps aux | grep -E 'java.*btr-platform' | grep -v grep | awk '{print $2, $9}'"))
print("== 8082 持有 ==")
print(run("ss -tlnp | grep 8082"))
print("== 新 jar 时间 ==")
print(run("ls -l --time-style=+%H:%M:%S /root/BRT/target/btr-platform.jar"))
ssh.close()
print("DONE")
