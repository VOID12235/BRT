# -*- coding: utf-8 -*-
"""部署 m.js + 全部 m/*.html + AuthService.java"""
import os, paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)
sftp = ssh.open_sftp()

LOCAL = r"D:\Users\a3614\Documents\默认项目"

# 前端
sftp.put(LOCAL + r"\frontend\m\m.js", "/var/www/btr/m/m.js")
for f in os.listdir(LOCAL + r"\frontend\m"):
    if f.endswith(".html"):
        sftp.put(LOCAL + r"\frontend\m" + os.sep + f, "/var/www/btr/m/" + f)
print("frontend UP")

# 后端源码
sftp.put(LOCAL + r"\src\main\java\com\btr\platform\service\AuthService.java",
         "/root/BRT/src/main/java/com/btr/platform/service/AuthService.java")
print("backend src UP")
sftp.close()

def run(c):
    i, o, e = ssh.exec_command(c, timeout=30)
    return o.read().decode("utf-8", "replace").strip()

print("== 服务器确认 ==")
print(run("grep -c 'BTRApp' /var/www/btr/m/m.js"))
print(run("grep -o 'm.js?v=[a-z0-9]*' /var/www/btr/m/login.html"))
print(run("grep -n 'exists-send\\|captchaEnabled ? \\\"flex\\\"' /root/BRT/src/main/java/com/btr/platform/service/AuthService.java | head -3"))
ssh.close()
print("DONE")
