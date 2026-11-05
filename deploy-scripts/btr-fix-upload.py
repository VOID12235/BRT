# -*- coding: utf-8 -*-
"""BTR 修复部署：上传后端改动 + 前端改动到服务器，并探测服务器构建环境。"""
import os
import paramiko
import sys

HOST = "103.236.92.193"
PORT = 45185
USER = "root"
PWD = "Q6Cq54BgvHp8"

ROOT = r"D:\Users\a3614\Documents\默认项目"
BACKEND = [
    "src/main/java/com/btr/platform/entity/Work.java",
    "src/main/java/com/btr/platform/dto/Requests.java",
    "src/main/java/com/btr/platform/dto/Views.java",
    "src/main/java/com/btr/platform/service/WorkService.java",
    "src/main/java/com/btr/platform/service/AuthService.java",
    "src/main/java/com/btr/platform/service/ContestService.java",
    "src/main/java/com/btr/platform/service/RankService.java",
    "src/main/java/com/btr/platform/controller/AuthController.java",
    "src/main/java/com/btr/platform/controller/RankController.java",
    "src/main/java/com/btr/platform/security/JwtAuthFilter.java",
]
# 前端：全部 html（版本号/SEO 已批量更新）+ 改过的 js + SEO 文件
FRONTEND_HTML = [
    "index.html", "about.html", "contest-list.html", "contest-detail.html",
    "rank.html", "notices.html", "portfolio.html", "signup.html",
    "work-submit.html", "login.html", "forgot-password.html", "user-center.html",
    "admin/admin-index.html", "admin/award-manage.html", "admin/contest-manage.html",
    "admin/export.html", "admin/judge-manage.html", "admin/logs.html",
    "admin/mails.html", "admin/notice-manage.html", "admin/rank-manage.html",
    "admin/settings.html", "admin/signup-manage.html", "admin/stats.html",
    "admin/user-manage.html", "admin/work-manage.html",
    "judge/judge-index.html", "judge/judge-score.html",
    "common/header.html", "common/footer.html",
    "m/index.html", "m/contests.html", "m/contest.html", "m/rank.html",
    "m/notices.html", "m/about.html", "m/signup.html", "m/work.html",
    "m/login.html", "m/center.html", "m/download.html",
]
FRONTEND_JS = [
    "assets/js/auth.js", "assets/js/contest-detail.js", "assets/js/rank.js", "m/m.js",
]
SEO_FILES = ["robots.txt", "sitemap.xml"]

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect(HOST, port=PORT, username=USER, password=PWD, timeout=30)
sftp = ssh.open_sftp()


def put(local, remote, must_exists=True):
    if not os.path.exists(local):
        print("MISS-LOCAL:", local)
        if must_exists:
            sys.exit(1)
        return
    # 远端目录确保存在
    d = os.path.dirname(remote)
    try:
        sftp.stat(d)
    except IOError:
        _mkdir_p(sftp, d)
    sftp.put(local, remote)
    print("PUT:", remote)


def _mkdir_p(sftp, remote_dir):
    parts = remote_dir.replace("\\", "/").split("/")
    cur = ""
    for p in parts:
        if not p:
            continue
        cur = cur + "/" + p
        try:
            sftp.stat(cur)
        except IOError:
            sftp.mkdir(cur)


print("== 后端 ==")
for rel in BACKEND:
    put(os.path.join(ROOT, rel), "/root/BRT/" + rel)

print("== 前端 html ==")
for rel in FRONTEND_HTML:
    put(os.path.join(ROOT, "frontend", rel), "/var/www/btr/" + rel)

print("== 前端 js ==")
for rel in FRONTEND_JS:
    put(os.path.join(ROOT, "frontend", rel), "/var/www/btr/" + rel)

print("== SEO 文件 ==")
for rel in SEO_FILES:
    put(os.path.join(ROOT, "frontend", rel), "/var/www/btr/" + rel)

# baidu 推送脚本放 /root/btr-seo/
try:
    sftp.stat("/root/btr-seo")
except IOError:
    sftp.mkdir("/root/btr-seo")
sftp.put(os.path.join(ROOT, "frontend", "baidu-push.py"), "/root/btr-seo/baidu-push.py")
print("PUT: /root/btr-seo/baidu-push.py")

print("== 服务器环境探测 ==")


def run(cmd):
    stdin, stdout, stderr = ssh.exec_command(cmd, timeout=120)
    out = stdout.read().decode("utf-8", "replace").strip()
    err = stderr.read().decode("utf-8", "replace").strip()
    return out, err


out, err = run("which mvn; mvn -v 2>&1 | head -3; echo ---; ls /root/BRT | head -20; echo ---; systemctl is-active btr; systemctl is-active clash")
print(out)
if err:
    print("STDERR:", err)

out, err = run("cat /etc/systemd/system/btr.service 2>/dev/null || cat /lib/systemd/system/btr.service 2>/dev/null")
print("--- btr.service ---")
print(out)

out, err = run("grep -n proxy_set_header /etc/nginx/conf.d/*.conf /etc/nginx/sites-enabled/* 2>/dev/null | head -20")
print("--- nginx proxy headers ---")
print(out)

sftp.close()
ssh.close()
print("DONE")
