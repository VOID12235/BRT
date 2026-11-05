# -*- coding: utf-8 -*-
"""补文件进服务器 git 仓库并 commit"""
import paramiko

ROOT = r"D:\Users\a3614\Documents\默认项目\frontend"
ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)
sftp = ssh.open_sftp()
sftp.put(ROOT + r"\assets\js\mobile-redirect.js", "/root/BRT/frontend/assets/js/mobile-redirect.js")
sftp.put(ROOT + r"\assets\img\btr-logo.svg", "/root/BRT/frontend/assets/img/btr-logo.svg")
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


run("cd /root/BRT && git add frontend/assets/js/mobile-redirect.js frontend/assets/img/btr-logo.svg && git commit -m 'fix: 补传 mobile-redirect.js 与 btr-logo.svg（手机端跳转与 logo 恢复）' && git log --oneline -1")
run("cd /root/BRT && git status --short | wc -l")

ssh.close()
print("DONE")
