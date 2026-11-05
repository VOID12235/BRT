# -*- coding: utf-8 -*-
import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)

def run(c):
    i, o, e = ssh.exec_command(c, timeout=30)
    return o.read().decode("utf-8", "replace").strip()

print(run("sed -n '70,115p' /root/BRT/src/main/java/com/btr/platform/service/FileService.java"))
print("== controller download 段 ==")
print(run("sed -n '80,115p' /root/BRT/src/main/java/com/btr/platform/controller/FileController.java"))
ssh.close()
print("DONE")
