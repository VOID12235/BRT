# -*- coding: utf-8 -*-
"""查 /ranks 500 错误"""
import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

def run(cmd, timeout=30):
    i, o, e = ssh.exec_command(cmd, timeout=timeout)
    return o.read().decode("utf-8", "replace").strip()

print(run("curl -s -m 8 'http://127.0.0.1:8082/api/v1/ranks?contestId=3' > /dev/null; sleep 1; grep -A25 'ERROR\\|Exception' /root/BRT/btr.log | grep -vE '^\\s*at (org|java|jdk|com.sun|sun|ch.qos|io.netty)' | tail -30"))
ssh.close()
print("DONE")
