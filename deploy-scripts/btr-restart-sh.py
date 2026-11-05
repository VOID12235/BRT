# -*- coding: utf-8 -*-
"""查看官方 restart.sh 启动命令"""
import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

def run(cmd, timeout=30):
    i, o, e = ssh.exec_command(cmd, timeout=timeout)
    return o.read().decode("utf-8", "replace").strip()

print(run("cat /root/BRT/deploy/restart.sh"))
print("---- docker-compose 端口映射 ----")
print(run("grep -A3 'ports' /root/BRT/deploy/docker-compose.yml | head -20"))
ssh.close()
print("DONE")
