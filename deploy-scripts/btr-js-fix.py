# -*- coding: utf-8 -*-
"""补传 forgot-password.js 与 m-login.js + 验证 + git commit"""
import paramiko

ROOT = r"D:\Users\a3614\Documents\默认项目\frontend\assets\js"
ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)
sftp = ssh.open_sftp()
for f in ["forgot-password.js", "m-login.js"]:
    sftp.put(ROOT + "\\" + f, "/var/www/btr/assets/js/" + f)
    sftp.put(ROOT + "\\" + f, "/root/BRT/frontend/assets/js/" + f)
print("PUT OK")
sftp.close()


def run(cmd, timeout=90):
    stdin, stdout, stderr = ssh.exec_command(cmd, timeout=timeout)
    out = stdout.read().decode("utf-8", "replace").strip()
    err = stderr.read().decode("utf-8", "replace").strip()
    print("$", cmd[:120])
    if out.strip():
        print(out[-1200:])
    if err.strip():
        print("ERR:", err[-400:])
    print("---")


# 验证外网可访问
run("curl -s -m 20 -o /dev/null -w 'FORGOT-JS:%{http_code}\\n' http://btr.navitaire.club:20757/assets/js/forgot-password.js")
run("curl -s -m 20 -o /dev/null -w 'M-LOGIN-JS:%{http_code}\\n' http://btr.navitaire.club:20757/assets/js/m-login.js")

# 模拟找回密码页加载（引用的脚本都在）
run("curl -s -m 20 http://btr.navitaire.club:20757/forgot-password.html | grep -oE 'src=\"[^\"]+\"' | head -8")

# git 提交
run("cd /root/BRT && git add frontend/assets/js/forgot-password.js frontend/assets/js/m-login.js && git commit -m 'fix: 补传 forgot-password.js 与 m-login.js（找回密码/手机登录按钮恢复）' && git log --oneline -1")
run("cd /root/BRT && git status --short | wc -l")

ssh.close()
print("DONE")
