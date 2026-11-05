# -*- coding: utf-8 -*-
"""评委分配满血版后端部署：sftp 传源码 → mvn package → systemctl restart → health"""
import paramiko
import time

HOST, PORT, USER, PWD = "103.236.92.193", 45185, "root", "Q6Cq54BgvHp8"
LOCAL = r"D:\Users\a3614\Documents\默认项目\src\main\java\com\btr\platform"
REMOTE = "/root/BRT/src/main/java/com/btr/platform"

FILES = [
    ("dto/Requests.java", "dto/Requests.java"),
    ("dto/Views.java", "dto/Views.java"),
    ("entity/Contest.java", "entity/Contest.java"),
    ("entity/JudgeAssign.java", "entity/JudgeAssign.java"),
    ("entity/Score.java", "entity/Score.java"),
    ("service/JudgeService.java", "service/JudgeService.java"),
    ("controller/AdminController.java", "controller/AdminController.java"),
]

cli = paramiko.SSHClient()
cli.set_missing_host_key_policy(paramiko.AutoAddPolicy())
cli.connect(HOST, port=PORT, username=USER, password=PWD, timeout=30)
sftp = cli.open_sftp()
for local_rel, remote_rel in FILES:
    lp = LOCAL.replace("/", "\\") + "\\" + local_rel.replace("/", "\\")
    rp = REMOTE + "/" + remote_rel
    sftp.put(lp, rp)
    print("PUT", remote_rel)
sftp.close()

stdin, stdout, stderr = cli.exec_command(
    "cd /root/BRT && mvn -q -DskipTests package 2>&1 | tail -5 && echo BUILD_DONE", timeout=420)
print(stdout.read().decode("utf-8", "replace")[-800:])

stdin, stdout, stderr = cli.exec_command("systemctl restart btr.service && echo RESTARTED", timeout=60)
print(stdout.read().decode("utf-8", "replace"))
time.sleep(22)
stdin, stdout, stderr = cli.exec_command("curl -s http://127.0.0.1:8082/api/v1/health", timeout=30)
print("HEALTH:", stdout.read().decode("utf-8", "replace")[:300])
cli.close()
