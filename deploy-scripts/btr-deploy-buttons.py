# -*- coding: utf-8 -*-
"""部署：m.js(b7)/m.css(m4)/rank.js(i)/rank.html + 15 个 m/*.html"""
import os, paramiko

FE = r"D:\Users\a3614\Documents\默认项目\frontend"
mdir = os.path.join(FE, "m")

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("103.236.92.193", port=45185, username="root", password="Q6Cq54BgvHp8", timeout=30)
sftp = ssh.open_sftp()

files = [
    (os.path.join(mdir, "m.js"), "/var/www/btr/m/m.js"),
    (os.path.join(mdir, "m.css"), "/var/www/btr/m/m.css"),
    (os.path.join(FE, "assets", "js", "rank.js"), "/var/www/btr/assets/js/rank.js"),
    (os.path.join(FE, "assets", "js", "dplayer.min.js"), "/var/www/btr/assets/js/dplayer.min.js"),
    (os.path.join(FE, "assets", "js", "docx-preview.min.js"), "/var/www/btr/assets/js/docx-preview.min.js"),
    (os.path.join(FE, "assets", "js", "jszip.min.js"), "/var/www/btr/assets/js/jszip.min.js"),
    (os.path.join(FE, "assets", "js", "xlsx.full.min.js"), "/var/www/btr/assets/js/xlsx.full.min.js"),
    (os.path.join(FE, "rank.html"), "/var/www/btr/rank.html"),
]
for name in os.listdir(mdir):
    if name.endswith(".html"):
        files.append((os.path.join(mdir, name), "/var/www/btr/m/" + name))

for lp, rp in files:
    sftp.put(lp, rp)
    print("上传统计", os.path.basename(lp))
sftp.close()

def run(c, timeout=30):
    i, o, e = ssh.exec_command(c, timeout=timeout)
    return o.read().decode("utf-8", "replace").strip()

print("== 服务器版本 ==")
print("rank.html:", run("grep -o 'rank.js?v=[0-9a-z]*' /var/www/btr/rank.html | head -1"))
print("m 页面 b7 数:", run("grep -rl 'm.js?v=20261105b7' /var/www/btr/m/ | wc -l"))
print("m.js 下载按钮:", run("grep -c '下载作品' /var/www/btr/m/m.js"))
print("rank.js 下载按钮:", run("grep -c '下载作品' /var/www/btr/assets/js/rank.js"))
ssh.close()
print("DONE")
