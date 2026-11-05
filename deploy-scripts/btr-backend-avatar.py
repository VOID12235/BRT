# -*- coding: utf-8 -*-
"""部署后端：上传修改的源码 -> mvn package -> systemctl restart -> health"""
import paramiko, os, time, sys

HOST, PORT, USER, PW = "103.236.92.193", 45185, "root", "Q6Cq54BgvHp8"
SRC = r"D:\Users\a3614\Documents\默认项目\src"
files = [
    ("main/java/com/btr/platform/dto/Requests.java", "/root/BRT/src/main/java/com/btr/platform/dto/Requests.java"),
    ("main/java/com/btr/platform/service/AuthService.java", "/root/BRT/src/main/java/com/btr/platform/service/AuthService.java"),
]

cli = paramiko.SSHClient()
cli.set_missing_host_key_policy(paramiko.AutoAddPolicy())
cli.connect(HOST, port=PORT, username=USER, password=PW, timeout=30)

def ssh(cmd, timeout=600):
    stdin, stdout, stderr = cli.exec_command(cmd, timeout=timeout)
    out = stdout.read().decode("utf-8", "replace")
    err = stderr.read().decode("utf-8", "replace")
    rc = stdout.channel.recv_exit_status()
    return rc, out, err

t = paramiko.Transport((HOST, PORT))
t.connect(username=USER, password=PW)
sftp = paramiko.SFTPClient.from_transport(t)
for local_rel, remote in files:
    local = os.path.join(SRC, local_rel.replace("/", os.sep))
    sftp.put(local, remote)
    print("UP", remote)
sftp.close()
t.close()

rc, out, err = ssh("cd /root/BRT && mvn -q -DskipTests package 2>&1 | tail -5")
print("BUILD rc:", rc)
print(out[-800:])

rc, out, err = ssh("systemctl restart btr.service && echo restarted")
print("RESTART:", out.strip())

time.sleep(25)
rc, out, err = ssh("curl -s http://127.0.0.1:8082/api/v1/health")
print("HEALTH:", out[:200])
cli.close()
print("DONE")
