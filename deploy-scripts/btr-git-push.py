# -*- coding: utf-8 -*-
"""上传 home.js + 服务器 git commit & push"""
import paramiko

HOST = "103.236.92.193"
PORT = 45185
USER = "root"
PWD = "Q6Cq54BgvHp8"
ROOT = r"D:\Users\a3614\Documents\默认项目"

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect(HOST, port=PORT, username=USER, password=PWD, timeout=30)
sftp = ssh.open_sftp()
sftp.put(ROOT + r"\frontend\assets\js\home.js", "/var/www/btr/assets/js/home.js")
print("PUT home.js")
sftp.close()


def run(cmd, timeout=300):
    stdin, stdout, stderr = ssh.exec_command(cmd, timeout=timeout)
    out = stdout.read().decode("utf-8", "replace")
    err = stderr.read().decode("utf-8", "replace")
    code = stdout.channel.recv_exit_status()
    print("$", cmd[:160])
    if out.strip():
        print(out[-2000:])
    if err.strip():
        print("ERR:", err[-800:])
    print("[exit=%s]" % code)


run("grep -c 'visibilitychange' /var/www/btr/assets/js/home.js")
run("cd /root/BRT && git status --short | head -50")
run("cd /root/BRT && git add -A && git commit -m 'fix: P0-P3 全量修复与生产加固（作品封面/状态机/榜单一致性/邮箱枚举/安全加固）+ SEO 收录准备（meta/robots/sitemap/百度推送脚本）' && git push origin main 2>&1 | tail -15", timeout=600)
run("cd /root/BRT && git log --oneline -3 && git status --short | wc -l")
ssh.close()
print("DONE")
