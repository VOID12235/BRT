# -*- coding: utf-8 -*-
import paramiko
ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)
def run(c, timeout=120):
    i, o, e = ssh.exec_command(c, timeout=timeout)
    return o.read().decode("utf-8", "replace").strip()
print("== 一级目录占用 ==")
print(run("du -x -d1 -h / 2>/dev/null | sort -rh | head -15"))
print("== /root 大头 ==")
print(run("du -x -d1 -h /root 2>/dev/null | sort -rh | head -8"))
ssh.close()
print("DONE")
