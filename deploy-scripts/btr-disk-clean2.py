# -*- coding: utf-8 -*-
import paramiko
ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)
def run(c, timeout=60):
    i, o, e = ssh.exec_command(c, timeout=timeout)
    return o.read().decode("utf-8", "replace").strip()
print(run("rm -f /root/images/*.tar; echo removed"))
print("== 磁盘 ==")
print(run("df -h / | tail -1"))
print("== 复测 rank.js ==")
print(run("curl -s -o /dev/null -w '%{http_code} %{size_download}B' http://127.0.0.1:20757/assets/js/rank.js?v=20261105k; echo"))
ssh.close()
print("DONE")
