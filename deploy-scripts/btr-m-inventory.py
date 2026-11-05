# -*- coding: utf-8 -*-
"""盘点 PC 端与手机端页面差异"""
import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)
cmds = [
    ("PC端HTML", "ls /var/www/btr/*.html 2>/dev/null | xargs -n1 basename"),
    ("手机端HTML", "ls /var/www/btr/m/*.html 2>/dev/null | xargs -n1 basename"),
    ("手机端JS", "ls /var/www/btr/m/*.js 2>/dev/null | xargs -n1 basename; ls /var/www/btr/m/assets/js/*.js 2>/dev/null | xargs -n1 basename"),
    ("PC端JS", "ls /var/www/btr/assets/js/*.js 2>/dev/null | xargs -n1 basename"),
]
for name, cmd in cmds:
    i, o, e = ssh.exec_command(cmd, timeout=60)
    print(f"=== {name} ===\n{o.read().decode('utf-8','replace').strip()}\n")
ssh.close()
print("DONE")
