# -*- coding: utf-8 -*-
"""本轮部署：同步本轮改动的 PC/手机前端文件到服务器 /var/www/btr/"""
import os, paramiko

REMOTE_ROOT = "/var/www/btr"

# (本地路径, 远程相对路径)
FILES = [
    (r"D:\Users\a3614\Documents\默认项目\frontend\m\m.js", "m/m.js"),
    (r"D:\Users\a3614\Documents\默认项目\frontend\m\contests.html", "m/contests.html"),
    (r"D:\Users\a3614\Documents\默认项目\frontend\assets\js\api.js", "assets/js/api.js"),
    (r"D:\Users\a3614\Documents\默认项目\frontend\assets\js\admin.js", "assets/js/admin.js"),
    (r"D:\Users\a3614\Documents\默认项目\frontend\assets\js\contest-detail.js", "assets/js/contest-detail.js"),
    (r"D:\Users\a3614\Documents\默认项目\frontend\assets\js\judge.js", "assets/js/judge.js"),
    (r"D:\Users\a3614\Documents\默认项目\frontend\assets\js\work-submit.js", "assets/js/work-submit.js"),
    (r"D:\Users\a3614\Documents\默认项目\frontend\assets\js\user-center.js", "assets/js/user-center.js"),
    (r"D:\Users\a3614\Documents\默认项目\frontend\admin\contest-manage.html", "admin/contest-manage.html"),
    (r"D:\Users\a3614\Documents\默认项目\frontend\contest-list.html", "contest-list.html"),
]

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)
sftp = ssh.open_sftp()

# 先看服务器现状
for cmd in [
    "ls /var/www/btr | head -30",
    "ls /var/www/btr/m | wc -l",
    "ls /var/www/btr/assets/js | wc -l",
]:
    i, o, e = ssh.exec_command(cmd, timeout=20)
    print("== " + cmd + " =>", o.read().decode("utf-8", "replace").strip())

print("---- 上传 ----")
for lp, rp in FILES:
    if not os.path.exists(lp):
        print("MISS", lp)
        continue
    sftp.put(lp, REMOTE_ROOT + "/" + rp)
    print("UP", rp, os.path.getsize(lp))

sftp.close()

# 校验远程文件时间戳与大小
print("---- 校验 ----")
i, o, e = ssh.exec_command(
    "ls -l --time-style=+%H:%M:%S /var/www/btr/m/m.js /var/www/btr/assets/js/api.js /var/www/btr/assets/js/admin.js /var/www/btr/contest-list.html /var/www/btr/admin/contest-manage.html",
    timeout=20,
)
print(o.read().decode("utf-8", "replace").strip())
ssh.close()
print("DONE")
