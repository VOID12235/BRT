# -*- coding: utf-8 -*-
import paramiko
ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)
def run(c, timeout=30):
    i, o, e = ssh.exec_command(c, timeout=timeout)
    return o.read().decode("utf-8", "replace").strip()
print("== nginx 状态 ==")
print(run("systemctl status nginx --no-pager 2>&1 | head -8"))
print("== 磁盘 ==")
print(run("df -h /var/www/btr | tail -2"))
print("== 文件 ==")
print(run("ls -la /var/www/btr/assets/js/ | head -10"))
print("== nginx error log 最近 ==")
print(run("tail -8 /var/log/nginx/error.log 2>/dev/null || tail -8 /var/log/nginx/*error* 2>/dev/null | head -10"))
ssh.close()
print("DONE")
